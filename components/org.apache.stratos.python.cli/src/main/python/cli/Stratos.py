import requests
import Configs
from cli.exceptions.AuthenticationError import AuthenticationError



class Stratos:
    """Apache Stratos Python API"""

    def __init__(self):
        pass

    @staticmethod
    def list_users():
        r = requests.get(Configs.stratos_api_url + 'users',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)

        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()

    @staticmethod
    def list_network_partitions():
        r = requests.get(Configs.stratos_api_url + 'networkPartitions',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        return r.json()

    @staticmethod
    def list_applications():
        r = requests.get(Configs.stratos_api_url + 'applications',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == "No applications found":
                return []
            else:
                raise requests.HTTPError()


    @staticmethod
    def list_cartridges():
        r = requests.get(Configs.stratos_api_url + 'cartridges',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        return r.json()

    @staticmethod
    def list_cartridges_groups():
        r = requests.get(Configs.stratos_api_url + 'cartridgeGroups',
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == "No cartridges found":
                return []
            else:
                raise requests.HTTPError()

    @staticmethod
    def deploy_user():
        raise ValueError


