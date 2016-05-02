import unittest

from marvin.deployDataCenter import DeployDataCenters

from lib.test_utils import MockApiClient


class TestDeployDataCenters(unittest.TestCase):

    def test_create_object(self):
        test_client = MockApiClient()
        deploy_data_centers = DeployDataCenters(test_client, {})

        self.assertIsNotNone(deploy_data_centers)


if __name__ == '__main__':
    unittest.main()
