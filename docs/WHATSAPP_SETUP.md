# 🔷 إرسال طلبات الشراء عبر واتساب - WhatsApp Click to Chat

## 📋 نظرة عامة
يستخدم النظام **WhatsApp Click to Chat** وهي خدمة **مجانية 100%** ولا تحتاج لأي API أو Tokens من Meta. كل ما تحتاجه هو فتح رابط في نافذة منبثقة داخل تطبيقك.

---

## 🚀 كيفية العمل

### 1️⃣ الباك إند (تم التنفيذ ✅)
الـ API يقوم بتحضير البيانات وإرجاعها للفرونت إند:
- **GET** `/api/purchase-orders/{id}/whatsapp?pharmacyId={pharmacyId}`

الـ Response:
```json
{
  "success": true,
  "data": {
    "phoneNumber": "201152784180",
    "encodedMessage": "%2A%F0%9F%93%8B...",
    "orderNumber": "PO-4-20260717110928"
  }
}
```

### 2️⃣ الفرونت إند (يجب تنفيذه)
استخدم الـ Response لفتح واتساب في **نافذة منبثقة داخل نفس الصفحة**:

```javascript
async function sendWhatsApp(orderId, pharmacyId) {
  try {
    // 1. استدعاء API الباك إند
    const response = await fetch(
      `/api/purchase-orders/${orderId}/whatsapp?pharmacyId=${pharmacyId}`
    );
    const result = await response.json();
    
    if (result.success) {
      const { phoneNumber, encodedMessage } = result.data;
      
      // 2. إنشاء رابط واتساب
      const whatsappUrl = `https://wa.me/${phoneNumber}?text=${encodedMessage}`;
      
      // 3. فتح في نافذة منبثقة داخل الصفحة (وليس نافذة متصفح جديدة)
      window.open(whatsappUrl, '_blank', 'width=600,height=700');
      // أو: window.location.href = whatsappUrl; // للتحويل المباشر
      
      // ✅ تم بنجاح
      showSuccessMessage("تم فتح واتساب لإرسال الطلب");
    }
  } catch (error) {
    showErrorMessage("فشل تحضير الرسالة: " + error.message);
  }
}
```

### ملاحظات مهمة:
- **لا حاجة** لأي Access Token أو API Keys من Meta
- **لا حاجة** لرقم واتساب Business
- الخدمة **مجانية بالكامل**
- فتح واتساب مباشر على رقم المورد بدون وسيط

---

## 📋 هيكل الـ API النهائي

| الطريقة | المسار | الوصف |
|---------|--------|-------|
| `GET` | `/api/purchase-orders/{id}/whatsapp?pharmacyId=` | تجهيز الرسالة ورقم الهاتف (للفرونت إند) |

## 📋 الـ Response
```json
{
  "success": true,
  "data": {
    "phoneNumber": "97466365442",
    "encodedMessage": "%2A%F0%9F%93%8B+%D8%B7%D9%84%D8%A8+%D8%B4%D8%B1%D8%A7%D8%A1+%D8%AC%D8%AF%D9%8A%D8%AF%2A...",
    "orderNumber": "PO-4-20260717110928"
  }
}
```

## 📋 مثال على شكل الرسالة في واتساب
```
*📋 طلب شراء جديد*
*Purchase Order* 🤝

*🏥 الصيدلية / Pharmacy:* صيدلية السلام
*📍 العنوان / Address:* 15 شارع النيل، القاهرة
*📞 الهاتف / Phone:* 01012345678

*📄 رقم الطلب / Order No:* PO-4-20260717110928
*📅 التاريخ / Date:* 2026-07-17
*🏪 المورد / Supplier:* شركة أمان للأدوية
*📊 الحالة / Status:* APPROVED
*⚠️ الأولوية / Priority:* URGENT

━━━━━━━━━━━━━━━━━━━━━━

*📦 المنتجات المطلوبة / Order Items*

1. *فولتارين 50 مجم*
   _الكمية (Qty):_ 200
   _السعر (Price):_ 💰 50.00 EGP
   _الإجمالي (Total):_ 💵 10000.00 EGP

━━━━━━━━━━━━━━━━━━━━━━

*💵 إجمالي الطلب / Total Amount:* *10000.00 EGP*

✅ *يرجى تأكيد الطلب في أقرب وقت*
 *للتواصل مع المورد (Contact Supplier):* 01152784180

_SmartPharma - إدارة الصيدليات الذكية_