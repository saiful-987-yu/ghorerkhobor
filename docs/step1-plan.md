# ঘরের খবর — ধাপ ১

## নাম ও থিম
- ইউজার অ্যাপ: **ঘরের খবর** (FamilyNotes)
- অ্যাডমিন অ্যাপ: **ঘরের খবর অ্যাডমিন**
- লোগো: ঘরের আকৃতির ভেতরে মেসেজ বাবল (`assets/logo.svg`)
- কালার: প্রাইমারি টিল `#0F766E`, গাঢ় টিল `#115E59`, অ্যাকসেন্ট কমলা `#F97316`, হালকা ব্যাকগ্রাউন্ড `#F0FDFA`, ডার্ক ব্যাকগ্রাউন্ড `#0B1F1E`

## গুগল শিটের কাঠামো
**Messages** শিট: id, device_id, person, app_package, app_name, sender, title, text, conversation, posted_at, posted_time, image_id, saved_at
**Devices** শিট: device_id, person, role, registered_at, last_seen, last_upload, app_version

ছবি শিটে থাকবে না। ছবি Google Drive-এর `GhorerKhobor_Images` ফোল্ডারে জমা হবে, শিটে শুধু image_id থাকবে।

## API (সব POST, JSON বডি)
ইউজার অ্যাপ (`key` লাগবে):
- `register`: device_id, person, role, app_version
- `upload`: device_id, items[] (id, app_package, app_name, sender, title, text, conversation, posted_at, image = ছোট JPEG base64)। একই id আবার এলে বাদ যাবে, একবারে সর্বোচ্চ ২০০টি।

অ্যাডমিন অ্যাপ (`adminKey` লাগবে):
- `devices`: সব ডিভাইসের তালিকা
- `list`: limit, after_seq (শুধু নতুনগুলো), before, device_id, app_package
- `image`: image_id দিলে ছবি base64-এ আসবে

## সেটআপ (আপনাকে একবার করতে হবে)
1. নতুন একটা Google Sheet বানান।
2. Extensions → Apps Script খুলে `backend/Code.gs` পুরোটা পেস্ট করে সেভ করুন।
3. উপরের ফাংশন তালিকা থেকে `setup` সিলেক্ট করে Run দিন। Sheets ও Drive পারমিশন Allow করুন।
4. Execution log-এ `API_KEY` ও `ADMIN_KEY` দেখা যাবে। পরে দেখতে চাইলে `showKeys` Run করুন।
5. Deploy → New deployment → Web app। Execute as: **Me**, Who has access: **Anyone**। যে URL পাবেন সেটা সংরক্ষণ করুন।
6. কোড বদলালে Manage deployments থেকে নতুন ভার্সন Deploy করতে হবে।

## ইমার্জেন্সি এক্সিট (ধাপ ২-৩ থেকে বাস্তবায়ন)
অ্যাপ এমনভাবে বানানো হবে যাতে ফোন আটকে যাওয়ার কোনো সুযোগই না থাকে:
- স্ক্রিনের ওপর ভাসমান লেয়ার (overlay), Accessibility Service, বা Device Admin ব্যবহার হবে না, তাই অ্যাপ স্ক্রিন লক করতে পারবে না এবং আনইনস্টলও সবসময় করা যাবে।
- অ্যাপে ইমার্জেন্সি বন্ধ বাটন থাকবে (লিসেনিং ও আপলোড সঙ্গে সঙ্গে বন্ধ)।
- পরপর কয়েকবার ক্র্যাশ করলে অ্যাপ নিজে থেকে কাজ বন্ধ করে দেবে।
- ফোন একদম আটকে গেলে পাওয়ার বাটন চেপে ধরে "Power off" লং-প্রেস করলে **Safe Mode** আসে। সেখানে থার্ড-পার্টি অ্যাপ চলে না, সেখান থেকে আনইনস্টল করা যায়। ফ্যাক্টরি রিসেট লাগবে না।
- Android-এ পাওয়ার বা ভলিউম বাটন সবখানে ধরার সুযোগ সাধারণ অ্যাপকে দেওয়া হয় না। তাই এই বাটন-ভিত্তিক বন্ধ করার কাজ Safe Mode-এর মাধ্যমে হবে।
