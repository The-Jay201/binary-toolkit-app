#!/bin/bash

echo -e "\033[1;36m=== ⚔️ AUTOMATIC APK UPDATER FOR THE-JAY201 ===\033[0m"

echo -e "\n\033[1;33m[?] Masukkan pesan pembaruan (contoh: 'Fix gradle build'):\033[0m"
read commit_msg
if [ -z "$commit_msg" ]; then
    commit_msg="Update konfigurasi Gradle via Termux"
fi

# Proses push otomatis memanfaatkan token yang sudah login
echo -e "\n\033[1;34m[*] Mengunggah kode perbaikan ke GitHub...\033[0m"
git add .
git commit -m "$commit_msg"
git push origin main

echo -e "\n\033[1;32m✔ Sukses Push! GitHub Actions sekarang sedang merakit APK baru.\033[0m"
echo -e "[*] Memantau proses compiler di server GitHub (Tekan Ctrl+C untuk keluar)..."
echo -e "------------------------------------------------------------"

# Menampilkan live progress build dari GitHub Actions langsung di Termux
sleep 3
gh run watch

