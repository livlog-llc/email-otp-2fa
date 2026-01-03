# otp-servlet-sample

Servlet API と `otp-web-servlet` を使ったシンプルなメールOTP 2FA サンプルです。Spring に依存せず、純粋な `HttpServlet` / `Filter` で構成しています。

## 動かし方

1. 依存を含めてビルドします。
   ```bash
   mvn -pl otp-servlet-sample -am clean package
   ```
2. Jetty を使って起動します。
   ```bash
   mvn -pl otp-servlet-sample -am jetty:run
   ```
3. ブラウザで `http://localhost:8080/login` を開き、下記でログインします。
   - ユーザーID: `user`
   - パスワード: `password`
4. 「確認コードを送信」を押すと OTP が生成され、コンソールログに出力されます。出力された6桁のコードを入力すると `/app` に遷移します。

## 実装のポイント

- `OtpServletInitializer` が `ServletContextListener` として `OtpStartServlet` / `OtpVerifyServlet` / `MfaEnforcerFilter` を登録します。
- OTP の保存はインメモリ H2、メール送信は `LoggingOtpMailer` でログ出力のみ行います。
- `/mfa` 画面はシンプルなHTMLを Servlet で返す実装です。JSPや静的HTMLに置き換えることもできます。
- MFA完了後のみ `/app` にアクセスできるよう、保護パスのプレフィックスはデフォルト設定（`/app`）を利用しています。
