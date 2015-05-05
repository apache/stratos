import os
from setuptools import setup


# Utility function to read the README file.
def read(file_name):
    return open(os.path.join(os.path.dirname(__file__), file_name)).read()

setup(
    name="stratos",
    version="0.0.5",
    author="Agentmilindu",
    author_email="Agentmilindu@gmail.com",
    py_modules=['stratos'],
    description="CLI for Apache Stratos",
    keywords="stratos",
    url="http://stratos.apache.org/",
    packages=['stratos'],
    install_requires=['cmd2', 'requests', 'texttable'],
    long_description=read('README.md'),
    classifiers=[
        "Development Status :: 1 - Planning",
        "Topic :: Utilities",
        "License :: OSI Approved :: Apache Software License",
    ],
    entry_points='''
        [console_scripts]
        stratos=src.Main:main
    ''',
)

