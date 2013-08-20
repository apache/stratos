<?php
// sample json

//$dd='{"pusher":{"name":"none"},"repository":{"name":"wordpress","has_wiki":true,"size":6668,"created_at":"2012-11-28T21:00:56-08:00","private":false,"watchers":0,"fork":false,"language":"PHP","url":"https://github.com/lakwarus/wordpress","id":6915450,"pushed_at":"2012-12-02T22:34:01-08:00","has_downloads":true,"open_issues":0,"has_issues":true,"forks":0,"stargazers":0,"description":"","owner":{"name":"lakwarus","email":"lakwarus@gmail.com"}},"forced":false,"after":"bf3e4bbe16336b272ca7230eecd1f42cd569d850","head_commit":{"modified":[],"added":["test.txt"],"timestamp":"2012-12-02T22:33:22-08:00","removed":[],"author":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"},"url":"https://github.com/lakwarus/wordpress/commit/bf3e4bbe16336b272ca7230eecd1f42cd569d850","id":"bf3e4bbe16336b272ca7230eecd1f42cd569d850","distinct":true,"message":">>>>>>>>>","committer":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"}},"deleted":false,"commits":[{"modified":["README.md"],"added":[],"timestamp":"2012-12-02T22:18:54-08:00","removed":[],"author":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"},"url":"https://github.com/lakwarus/wordpress/commit/7afd4089f402cc9926aa5490cbcef21d77f63265","id":"7afd4089f402cc9926aa5490cbcef21d77f63265","distinct":true,"message":"add","committer":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"}},{"modified":["README.md"],"added":[],"timestamp":"2012-12-02T22:22:11-08:00","removed":[],"author":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"},"url":"https://github.com/lakwarus/wordpress/commit/817137d6f40ff0065d1687437a67c2bb42ea98c8","id":"817137d6f40ff0065d1687437a67c2bb42ea98c8","distinct":true,"message":"add","committer":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"}},{"modified":[],"added":["test.txt"],"timestamp":"2012-12-02T22:33:22-08:00","removed":[],"author":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"},"url":"https://github.com/lakwarus/wordpress/commit/bf3e4bbe16336b272ca7230eecd1f42cd569d850","id":"bf3e4bbe16336b272ca7230eecd1f42cd569d850","distinct":true,"message":">>>>>>>>>","committer":{"name":"Lakmal Warusawithana","username":"lakwarus","email":"lakmal@wso2.com"}}],"ref":"refs/heads/master","before":"d5d7e4ab8ae58af96dd151bdf536644323e901e3","compare":"https://github.com/lakwarus/wordpress/compare/d5d7e4ab8ae5...bf3e4bbe1633","created":false}';


$json = json_decode($_POST['payload']);
#$json = json_decode($dd);
$repository=$json->repository;

$jsonIterator = new RecursiveIteratorIterator(
    new RecursiveArrayIterator(json_decode(json_encode($repository), TRUE)),
    RecursiveIteratorIterator::SELF_FIRST);

foreach ($jsonIterator as $key => $val) {
    if(is_array($val)) {
    } else {
        if($key=="url"){
	    $giturl=$val;	
	}
    }
}
#echo $giturl;

$xml="<?xml version=\"1.0\"?><soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:xsd=\"http://org.apache.axis2/xsd\"><soapenv:Header/><soapenv:Body><xsd:synchronize><xsd:repositoryURL>".$giturl."</xsd:repositoryURL></xsd:synchronize></soapenv:Body></soapenv:Envelope>";

$url = 'https://203.143.18.246:9445/services/RepoNotificationService';
$ch = curl_init($url);
curl_setopt($ch, CURLOPT_MUTE, 1);
curl_setopt($ch, CURLOPT_SSL_VERIFYHOST, 0);
curl_setopt($ch, CURLOPT_SSL_VERIFYPEER, 0);
curl_setopt($ch, CURLOPT_POST, 1);
curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type: text/xml'));
curl_setopt($ch, CURLOPT_POSTFIELDS, "$xml");
curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);

$response = curl_exec($ch);

curl_close($ch);

?>
