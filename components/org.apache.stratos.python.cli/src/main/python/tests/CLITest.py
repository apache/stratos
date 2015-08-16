from scripttest import TestFileEnvironment

env = TestFileEnvironment('./scratch')


def test_list_users():
    env.clear()
    result = env.run('stratos-cli list-users')
    assert result.stdout.startswith("")

test_list_users()