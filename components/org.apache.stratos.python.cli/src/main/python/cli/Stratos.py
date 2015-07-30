import requests
import Configs
from cli.exceptions.AuthenticationError import AuthenticationError



class Stratos:
    """Apache Stratos Python API"""

    def __init__(self):
        pass

    """
    # Users

    """
    @staticmethod
    def list_users():
        return Stratos.get('users', errorMessage='No applications found')

    @staticmethod
    def add_users(username, password, role_name, first_name, last_name, email, profile_name):
        data = [username]
        r = requests.post(Configs.stratos_api_url + 'users', data,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)

    """
    # Network Partitions

    """
    @staticmethod
    def list_network_partitions():
        return Stratos.get('networkPartitions', errorMessage='No network partitions found')

    """
    # Applications

    """
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

    """
    # Cartridges

    """
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

    """
    # Kubernetes Clusters

    """
    @staticmethod
    def list_kubernetes_clusters():
        return Stratos.get('kubernetesClusters', errorMessage='Kubernetes cluster not found')


    """
    # Utils

    """
    @staticmethod
    def get(resource, errorMessage):
        r = requests.get(Configs.stratos_api_url + resource,
                         auth=(Configs.stratos_username, Configs.stratos_password), verify=False)
        print(r.text)
        if r.status_code == 200:
            return r.json()
        elif r.status_code == 400:
            raise requests.HTTPError()
        elif r.status_code == 401:
            raise AuthenticationError()
        elif r.status_code == 404:
            if r.json() and r.json()['errorMessage'] == errorMessage:
                return []
            else:
                raise requests.HTTPError()




