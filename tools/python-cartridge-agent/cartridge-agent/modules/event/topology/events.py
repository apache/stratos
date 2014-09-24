import json


class MemberActivatedEvent:

    def __init__(self):
        pass

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberActivatedEvent()


class MemberTerminatedEvent:

    def __init__(self):
        pass

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberTerminatedEvent()


class MemberSuspendedEvent:

    def __init__(self):
        pass

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberSuspendedEvent()


class CompleteTopologyEvent:

    def __init__(self):
        pass

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = CompleteTopologyEvent()


class MemberStartedEvent:

    def __init__(self):
        pass

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        instance = MemberStartedEvent()


