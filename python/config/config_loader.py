import yaml
from typing import Dict, Any

class ConfigLoader:
    _instance = None
    _config: Dict[str, Any] = {}

    def __new__(cls):
        if cls._instance is None:
            cls._instance = super(ConfigLoader, cls).__new__(cls)
            cls._instance._load_config()
        return cls._instance

    def _load_config(self):
        try:
            with open('config/config.yaml', 'r', encoding='utf-8') as f:
                self._config = yaml.safe_load(f)
        except FileNotFoundError:
            raise Exception("配置文件 config.yaml 未找到")
        except yaml.YAMLError as e:
            raise Exception(f"配置文件解析错误: {e}")

    @property
    def cos_config(self) -> Dict[str, str]:
        return self._config.get('cos', {})

    @property
    def rabbitmq_config(self) -> Dict[str, Any]:
        return self._config.get('rabbitmq', {})

# 单例模式获取配置
config = ConfigLoader()