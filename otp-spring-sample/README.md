# otp-spring-sample

`otp-spring-sample` は `otp-web-spring` を使った Spring Boot アプリのサンプルです。固定ユーザーでログインした後にメールOTPによるMFAを行う最小構成を確認できます。

## 前提
- Java 17
- Maven 3.x

## 起動方法
```bash
# 依存を含めてビルド
mvn -pl otp-spring-sample -am clean package

# 開発サーバーの起動（デフォルト: http://localhost:8080）
mvn -pl otp-spring-sample spring-boot:run
```

## 操作手順
1. `http://localhost:8080/login` を開き、`user` / `password` でログインします（`LoginController` で固定認証）。
2. `/mfa` 画面で「Send OTP」を押し、コンソールに出力されたコードを確認します（`LoggingOtpMailer` がメールの代わりにログ出力）。
3. コードを `/mfa/verify` に入力して検証すると、保護画面 `/app` に遷移します。

## 設定のポイント
- **データベース**: `application.yml` によりインメモリH2を使用し、`schema.sql` が起動時に自動適用されます。外部DBを使う場合は `spring.datasource.*` を上書きしてください。
- **メール送信**: デフォルトは `LoggingOtpMailer` によるログ出力です。実際のSMTP送信に切り替える場合は `OtpBeansConfig` のコメントアウト部分を有効化して `SimpleJavaMailOtpMailer` を設定します。
- **Webパス/リダイレクト**: MFA画面、送信/検証エンドポイント、成功/失敗リダイレクト、保護パスは `application.yml` の `otp.web` セクションで変更できます。
- **ログイン処理のフック**: パスワード認証成功時に `OtpWebSupport.markPasswordOk(session, userId, email)` を呼び、セッションに `USER_ID` / `USER_EMAIL` / `PASSWORD_OK` をセットしています。自身の認証処理に組み込む際の参考にしてください。

## 主要ファイル
- `src/main/java/jp/livlog/otp/sample/OtpSampleApplication.java`: Spring Boot エントリーポイント
- `src/main/java/jp/livlog/otp/sample/config/DataSourceConfig.java`: データソース設定（H2と外部DBの切替）
- `src/main/java/jp/livlog/otp/sample/config/OtpBeansConfig.java`: OTPストア・メーラーのBean定義
- `src/main/java/jp/livlog/otp/sample/web/*.java`: ログイン/MFA/保護画面のController群
- `src/main/resources/templates/*.html`: ログイン、MFA、保護画面のテンプレート
- `src/main/resources/application.yml`: ポートやOTP Web設定
- `src/main/resources/schema.sql`: OTPテーブル定義
