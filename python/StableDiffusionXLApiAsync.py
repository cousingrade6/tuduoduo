import json
import os
import time
os.environ['CUDA_VISIBLE_DEVICES'] = '3'
from fastapi import FastAPI
from diffusers import StableDiffusionXLPipeline
import io
from datetime import datetime
from qcloud_cos import CosConfig, CosS3Client
import uuid
import os
import pika

from config.config_loader import config


app = FastAPI()

# 腾讯云COS配置
COS_CONFIG = config.cos_config

# 新增RabbitMQ配置
RABBITMQ_CONFIG = config.rabbitmq_config

class ImageGenerator:
    def __init__(self):
        self.cos_client = CosS3Client(CosConfig(
            Region=COS_CONFIG["region"],
            SecretId=COS_CONFIG["secret_id"],
            SecretKey=COS_CONFIG["secret_key"]
        ))

        self.setup_rabbitmq()

        self.pipe = StableDiffusionXLPipeline.from_pretrained(
            "/mnt/d/ckpts/stable-diffusion-xl-base-1.0",
            # torch_dtype=torch.float16,
            # variant="fp16"
        ).to("cuda")

    def setup_rabbitmq(self):
        """初始化RabbitMQ连接和队列"""
        self.connection = pika.BlockingConnection(
            pika.ConnectionParameters(
                host=RABBITMQ_CONFIG["host"],
                port=RABBITMQ_CONFIG["port"],
                virtual_host=RABBITMQ_CONFIG["vhost"],
                credentials=pika.PlainCredentials(
                    RABBITMQ_CONFIG["user"],
                    RABBITMQ_CONFIG["password"]
                )
            )
        )
        self.channel = self.connection.channel()

        # 声明持久化任务优先级队列
        self.channel.queue_declare(
            queue=RABBITMQ_CONFIG["task_queue"],
            durable=True,
            arguments={"x-max-priority": 9}, # 设置优先级
        )

        # 声明持久化结果队列
        self.channel.queue_declare(
            queue='image_generation_results',
            durable=True
        )

    def upload_to_cos(self, image_bytes: bytes) -> str:
        """上传图片到腾讯云COS"""
        object_key = f"generated-images/{datetime.now().strftime('%Y%m%d')}/{uuid.uuid4().hex}.png"
        self.cos_client.put_object(
            Bucket=COS_CONFIG["bucket"],
            Body=image_bytes,
            Key=object_key,
            StorageClass='STANDARD',
            ContentType="image/png"
        )
        return f"https://{COS_CONFIG['bucket']}.cos.{COS_CONFIG['region']}.myqcloud.com/{object_key}"

    def process_task(self, ch, method, properties, body):
        """处理图像生成任务"""
        try:
            message = json.loads(body)
            task_id = message["task_id"]
            prompt = message["prompt"]
            negative_prompt = message.get("negative_prompt")
            steps = message.get("steps", 50)
            width = message.get("width", 1024)
            height = message.get("height", 1024)

            print(f"Processing task {task_id}")

            # 生成图片
            start_time = time.time()
            image = self.pipe(
                prompt=prompt,
                negative_prompt=negative_prompt,
                num_inference_steps=steps,
                width=width,
                height=height
            ).images[0]

            # 转换为字节流并上传
            img_byte_arr = io.BytesIO()
            image.save(img_byte_arr, format="PNG")
            image_url = self.upload_to_cos(img_byte_arr.getvalue())

            # 准备结果
            result = {
                "task_id": task_id,
                "status": "SUCCESS",
                "image_url": image_url,
                "generation_time": round(time.time() - start_time, 2),
                "timestamp": datetime.now().isoformat()
            }

            # 发布结果到结果交换机，使用task_id作为routing_key
            self.channel.basic_publish(
                exchange='',  # 空字符串表示默认交换机
                routing_key='image_generation_results',
                body=json.dumps(result),
                properties=pika.BasicProperties(
                    correlation_id=task_id,  # 关键：回传相同的correlationId
                    delivery_mode=2,
                    content_type='application/json'
                )
            )
            ch.basic_ack(delivery_tag=method.delivery_tag)

        except Exception as e:
            error_result = {
                "task_id": task_id,
                "status": "ERROR",
                "error": str(e),
                "timestamp": datetime.now().isoformat()
            }

            self.channel.basic_publish(
                exchange='',  # 空字符串表示默认交换机
                routing_key='image_generation_results',
                body=json.dumps(error_result)
            )

            ch.basic_ack(delivery_tag=method.delivery_tag)

    def start_consuming(self):
        """启动消费者"""
        self.channel.basic_qos(prefetch_count=1)
        self.channel.basic_consume(
            queue=RABBITMQ_CONFIG["task_queue"],
            on_message_callback=self.process_task,
            consumer_tag='image_generator_worker'
        )

        print(" [*] Waiting for messages. To exit press CTRL+C")
        self.channel.start_consuming()

if __name__ == "__main__":
    generator = ImageGenerator()
    generator.start_consuming()

