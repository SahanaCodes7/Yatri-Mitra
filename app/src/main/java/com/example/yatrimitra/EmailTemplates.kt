package com.example.yatrimitra

object EmailTemplates {
    fun getWelcomeEmailHtml(name: String, email: String, joinDate: String): String = """
<!DOCTYPE html>
<html>
<body style="margin:0;padding:0;background-color:#0D1B2A;font-family:Arial,sans-serif;">
<table width="100%" cellpadding="0" cellspacing="0" style="background-color:#0D1B2A;">
  <tr><td align="center" style="padding:20px 10px;">
    <table width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;">
      <!-- HEADER -->
      <tr><td style="background-color:#1A2E40;border-radius:14px 14px 0 0;padding:28px;text-align:center;border:1px solid #1A3550;">
        <p style="font-size:36px;margin:0 0 8px 0;">🛺</p>
        <h1 style="color:#1D9E75;font-size:24px;margin:0;letter-spacing:1px;">YatriMitra</h1>
        <p style="color:#7FA8C0;font-size:13px;margin:6px 0 0 0;">Your Smart Ride Companion</p>
      </td></tr>
      <!-- HERO -->
      <tr><td style="background-color:#1A2E40;padding:32px 28px;text-align:center;border-left:1px solid #1A3550;border-right:1px solid #1A3550;">
        <h2 style="color:#E8F1FF;font-size:22px;margin:0 0 12px 0;">You&#39;re all set, ${name}! 🎉</h2>
        <p style="color:#7FA8C0;font-size:14px;margin:0;line-height:1.6;">Your YatriMitra account is ready. Start tracking autos and booking rides in Bengaluru instantly.</p>
      </td></tr>
      <!-- ACCOUNT SUMMARY -->
      <tr><td style="background-color:#122030;padding:20px 28px;border-left:1px solid #1A3550;border-right:1px solid #1A3550;">
        <p style="color:#3A5270;font-size:10px;letter-spacing:2px;text-transform:uppercase;margin:0 0 12px 0;">ACCOUNT DETAILS</p>
        <table width="100%" cellpadding="6" cellspacing="0">
          <tr><td style="color:#7FA8C0;font-size:12px;">Email</td><td style="color:#E8F1FF;font-size:12px;text-align:right;">${email}</td></tr>
          <tr><td style="color:#7FA8C0;font-size:12px;">Role</td><td style="color:#E8F1FF;font-size:12px;text-align:right;">Passenger</td></tr>
          <tr><td style="color:#7FA8C0;font-size:12px;">Joined</td><td style="color:#E8F1FF;font-size:12px;text-align:right;">${joinDate}</td></tr>
        </table>
      </td></tr>
      <!-- FEATURES -->
      <tr><td style="background-color:#1A2E40;padding:24px 28px;border-left:1px solid #1A3550;border-right:1px solid #1A3550;">
        <p style="color:#3A5270;font-size:10px;letter-spacing:2px;text-transform:uppercase;margin:0 0 16px 0;">WHAT YOU CAN DO</p>
        <table width="100%" cellpadding="0" cellspacing="0">
          <tr><td style="padding:0 0 12px 0;">
            <table width="100%" cellpadding="12" cellspacing="0" style="background-color:#0F4A3A;border-radius:10px;border:1px solid #1D9E75;">
              <tr><td><p style="color:#1D9E75;font-size:14px;font-weight:bold;margin:0 0 4px 0;">🗺️ Track Autos</p><p style="color:#7FA8C0;font-size:12px;margin:0;">Watch auto-rickshaws move in real-time on an interactive map with live ETA.</p></td></tr>
            </table>
          </td></tr>
          <tr><td style="padding:0 0 12px 0;">
            <table width="100%" cellpadding="12" cellspacing="0" style="background-color:#122030;border-radius:10px;border:1px solid #1A3550;">
              <tr><td><p style="color:#EF9F27;font-size:14px;font-weight:bold;margin:0 0 4px 0;">📋 Trip History</p><p style="color:#7FA8C0;font-size:12px;margin:0;">Every ride you take is saved. Review your trips, routes, and spending anytime.</p></td></tr>
            </table>
          </td></tr>
          <tr><td>
            <table width="100%" cellpadding="12" cellspacing="0" style="background-color:#0C447C;border-radius:10px;border:1px solid #185FA5;">
              <tr><td><p style="color:#185FA5;font-size:14px;font-weight:bold;margin:0 0 4px 0;">🛡️ Safety Contacts</p><p style="color:#7FA8C0;font-size:12px;margin:0;">Add trusted contacts. They&#39;ll be alerted instantly in an emergency via SOS.</p></td></tr>
            </table>
          </td></tr>
        </table>
      </td></tr>
      <!-- CTA -->
      <tr><td style="background-color:#1A2E40;padding:24px 28px;text-align:center;border-left:1px solid #1A3550;border-right:1px solid #1A3550;">
        <table cellpadding="0" cellspacing="0" style="margin:0 auto;">
          <tr><td style="background-color:#1D9E75;border-radius:12px;padding:14px 32px;">
            <p style="color:#0D1B2A;font-size:15px;font-weight:bold;margin:0;">Open YatriMitra</p>
          </td></tr>
        </table>
      </td></tr>
      <!-- FOOTER -->
      <tr><td style="background-color:#0D1B2A;padding:20px 28px;text-align:center;border-radius:0 0 14px 14px;border:1px solid #1A3550;">
        <p style="color:#3A5270;font-size:11px;margin:0 0 4px 0;">This is an automated message. Please do not reply.</p>
        <p style="color:#3A5270;font-size:11px;margin:0;">&#169; 2026 YatriMitra · Bengaluru, Karnataka</p>
      </td></tr>
    </table>
  </td></tr>
</table>
</body>
</html>
""".trimIndent()
}
