import requests
import Configs


class Stratos:
    """Apache Stratos Python API"""

    def __init__(self):
        pass

    @staticmethod
    def list_users():
        r = requests.get(Configs.stratos_url + 'users',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        if r.status_code is 200:
            return r.json()

        elif r.status_code is 400:
            raise requests.HTTPError()

    @staticmethod
    def network_partitions():
        pass



