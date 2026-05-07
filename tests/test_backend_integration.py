import requests
import json
import time

def test_backend_health():
    """Test if backend is running and responsive"""
    try:
        response = requests.get("http://localhost:5000/api/", timeout=5)
        assert response.status_code == 200
        assert response.json() == {"message": "Hello World"}
        print("✅ Backend health check passed")
        return True
    except Exception as e:
        print(f"❌ Backend health check failed: {e}")
        return False

def test_status_endpoint():
    """Test status check endpoint"""
    try:
        # Test POST
        post_data = {"client_name": "test_client"}
        response = requests.post("http://localhost:5000/api/status", json=post_data, timeout=5)
        assert response.status_code == 200
        status_data = response.json()
        assert "id" in status_data
        assert status_data["client_name"] == "test_client"
        print("✅ Status POST endpoint passed")
        
        # Test GET
        response = requests.get("http://localhost:5000/api/status", timeout=5)
        assert response.status_code == 200
        status_list = response.json()
        assert isinstance(status_list, list)
        print("✅ Status GET endpoint passed")
        return True
    except Exception as e:
        print(f"❌ Status endpoint test failed: {e}")
        return False

if __name__ == "__main__":
    print("🧪 Running backend integration tests...")
    
    if test_backend_health():
        test_status_endpoint()
    
    print("🏁 Integration tests completed")
