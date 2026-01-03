package jp.livlog.otp.servletsample;

public final class PageTemplates {

    private PageTemplates() {}

    public static String login(String ctx, boolean error) {
        String errorBlock = error ? "<p class=\"error\">認証に失敗しました。</p>" : "";
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <head>
                  <meta charset="UTF-8" />
                  <title>Login - OTP Servlet Sample</title>
                  <style>
                    body { font-family: sans-serif; max-width: 560px; margin: 40px auto; }
                    form { display: grid; gap: 12px; }
                    label { font-weight: bold; }
                    input { padding: 8px; font-size: 16px; }
                    .error { color: #c00; }
                    .card { border: 1px solid #ddd; padding: 24px; border-radius: 8px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>ログイン</h1>
                    <p>ユーザーID: <code>user</code> / パスワード: <code>password</code></p>
                    %s
                    <form method="post" action="%s/login">
                      <label for="userId">ユーザーID</label>
                      <input id="userId" name="userId" required />
                      <label for="password">パスワード</label>
                      <input type="password" id="password" name="password" required />
                      <button type="submit">ログイン</button>
                    </form>
                  </div>
                </body>
                </html>
                """.formatted(errorBlock, ctx);
    }

    public static String mfa(String ctx, boolean error) {
        String errorBlock = error ? "<p class=\"error\">認証に失敗しました。再度お試しください。</p>" : "";
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <head>
                  <meta charset="UTF-8" />
                  <title>MFA - OTP Servlet Sample</title>
                  <style>
                    body { font-family: sans-serif; max-width: 560px; margin: 40px auto; }
                    form { display: grid; gap: 12px; }
                    label { font-weight: bold; }
                    input { padding: 8px; font-size: 16px; }
                    .error { color: #c00; }
                    .card { border: 1px solid #ddd; padding: 24px; border-radius: 8px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>メールOTPの確認</h1>
                    <p>確認コードはコンソールログに出力されます。</p>
                    %s
                    <form method="post" action="%s/mfa/start">
                      <button type="submit">確認コードを送信</button>
                    </form>
                    <hr />
                    <form method="post" action="%s/mfa/verify">
                      <label for="otp">確認コード（6桁）</label>
                      <input id="otp" name="otp" inputmode="numeric" pattern="[0-9]{6}" maxlength="6" required />
                      <button type="submit">確認</button>
                    </form>
                  </div>
                </body>
                </html>
                """.formatted(errorBlock, ctx, ctx);
    }

    public static String app(String ctx) {
        return """
                <!DOCTYPE html>
                <html lang="ja">
                <head>
                  <meta charset="UTF-8" />
                  <title>App - OTP Servlet Sample</title>
                  <style>
                    body { font-family: sans-serif; max-width: 560px; margin: 40px auto; }
                    .card { border: 1px solid #ddd; padding: 24px; border-radius: 8px; }
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>アプリケーション</h1>
                    <p>MFAが完了したので、このページにアクセスできます。</p>
                    <p><a href="%s/login">ログイン画面に戻る</a></p>
                  </div>
                </body>
                </html>
                """.formatted(ctx);
    }
}
