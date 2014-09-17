import json


class InstanceCleanupMemberEvent:
    def __init__(self, member_id):
        self.member_id = member_id

    @staticmethod
    def create_from_json(json_str):
        json_obj = json.loads(json_str)
        m_id = json_obj["memberId"] if "memberId" in json_obj else None

        return InstanceCleanupMemberEvent(m_id)