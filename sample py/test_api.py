import urllib.request
import json

url = "http://127.0.0.1:8000/predict_stress"

def send_request(data, name):
    print(f"Testing {name}...")
    req = urllib.request.Request(url)
    req.add_header('Content-Type', 'application/json')
    jsondata = json.dumps(data).encode('utf-8')
    req.add_header('Content-Length', len(jsondata))
    
    try:
        response = urllib.request.urlopen(req, jsondata)
        res_body = response.read()
        print("Status Code:", response.getcode())
        print("Response:", json.loads(res_body.decode('utf-8')))
    except urllib.error.HTTPError as e:
        print("HTTP Error:", e.code)
        print("Response:", e.read().decode('utf-8'))
    except Exception as e:
        print("Error:", e)
    print("-" * 20)

# Test Case 1: RF Model (with EDA)
payload_rf = {
    "eda_mean": 0.5, "eda_std": 0.1, "eda_min": 0.2, "eda_max": 0.8,
    "bvp_mean": 0.5, "bvp_std": 0.1,
    "temp_mean": 30.0, "temp_std": 0.5,
    "acc_mag_mean": 1.0, "acc_mag_std": 0.1
}

# Test Case 2: Lite Model (without EDA)
payload_lite = {
    "bvp_mean": 0.5, "bvp_std": 0.1,
    "bvp_min": 0.0, "bvp_max": 1.0, "bvp_range": 1.0, "bvp_energy": 0.5,
    "acc_mean": 0.5, "acc_std": 0.1, "acc_max": 1.0
}

send_request(payload_rf, "RF Model")
send_request(payload_lite, "Lite Model")
