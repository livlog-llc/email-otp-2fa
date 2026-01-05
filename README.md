# email-otp-2fa

メールによるワンタイムパスワード（OTP）を使った二要素認証を、既存のWebアプリに後付けするためのコンポーネント群です。Java 17 / Maven を前提に、OTP生成・保存・送信・Web連携をモジュール単位で提供します。

## モジュール構成
- **otp-core**: OTP生成・検証ポリシーなど純粋なロジックを提供。
- **otp-storage**: OTP状態の永続化層。JDBC実装（H2など）を含みます。
- **otp-mail**: OTPメール送信用のインターフェースと実装（Simple Java Mail / ログ出力など）。
- **otp-web-spring**: Spring Boot / MVC 向けの組み込み用ライブラリ（Controller / Filter / Auto Configuration）。
- **otp-web-servlet**: Servlet ベースのアプリ向けの組み込み用ライブラリ（HttpServlet / Filter）。
- **otp-spring-sample**: 上記ライブラリを組み込んだサンプル（Spring Boot）アプリ。参考実装として利用できます。
- **otp-servlet-sample**: Servlet API のみで構成したシンプルなサンプルアプリ。Jettyで手軽に動作確認できます。

## 開発環境の前提
- Java 17
- Maven 3.x

リポジトリ直下で `mvn clean install` を実行すると全モジュールをビルドできます。個別モジュールだけビルドしたい場合は `-pl <module> -am` を付けてください。

### 親POMのインストールとリリース手順
マルチモジュール構成のため、まず親POMをローカルリポジトリにインストールしてから、Maven Release Plugin を使ってタグ付け・デプロイを行います。

1. **親POMをインストール**（プロジェクトルートで実行）
   ```bash
   mvn -N clean install
   ```
   これで `jp.livlog:email-otp-2fa:0.1.0-SNAPSHOT` の親POMがローカルに登録され、個別モジュールのビルドや release:prepare で参照できるようになります。

2. **事前確認**
   - `git status` がクリーンであることを確認
   - SNAPSHOT バージョンのままになっているか確認
   - ルートディレクトリで `mvn clean install` を一度実行してビルドを通しておく

3. **リリース準備（バージョン確定とタグ作成）**
   ```bash
   mvn release:prepare
   ```
   ルート `pom.xml` の `<scm>` 設定を基に、タグ（形式 `v<version>`）とリリース用コミットが生成され、`release.properties` が作成されます。

4. **リリース実行（デプロイ）**
   ```bash
   mvn release:perform
   ```
   `release.properties` に記録された SCM URL から再チェックアウトし、`deploy` ゴールまで実行します。`release:prepare` を先に実行していない場合は `No SCM URL was provided to perform the release` のようなエラーになるため、必ず手順3を完了させてください。

5. **タグとブランチのプッシュ、GitHub Release 作成**
   - `git push origin <ブランチ>` / `git push origin v<version>`
   - GitHub 上で該当タグの Release を公開し、JitPack のビルドをトリガーする

## otp-spring-sample の実行方法
1. 依存を含めてビルドし、そのまま Spring Boot を起動します。
   ```bash
   mvn -pl otp-spring-sample -am clean package
   mvn -pl otp-spring-sample spring-boot:run
   ```
2. ブラウザで `http://localhost:8080/login` を開き、以下の固定ユーザーでログインします。
   - ユーザーID: `user`
   - パスワード: `password`
3. 「Send OTP」を押して確認コードを送信し、ログ出力されたコードで `/mfa/verify` を通過すると `/app` に遷移します。

### サンプルの設定ポイント
- デフォルトのデータベースはインメモリH2で、`schema.sql` が自動適用されます（表定義の参考にもなります）。
- メール送信は `LoggingOtpMailer` によりコンソールに出力するだけです。SMTPで実送信したい場合は `otp-spring-sample/src/main/java/jp/livlog/otp/sample/config/OtpBeansConfig.java` のコメントを外して `SimpleJavaMailOtpMailer` を有効化します。
- Webパスやリダイレクト先は `otp-spring-sample/src/main/resources/application.yml` で設定しています。フィルタ保護パスの例やリダイレクト先の変更方法の参考にしてください。

## プロジェクトへの導入方法
### Spring Boot / Spring MVC（otp-web-spring）
1. `pom.xml` に依存を追加します。
   ```xml
   <dependency>
     <groupId>jp.livlog</groupId>
     <artifactId>otp-web-spring</artifactId>
     <version>0.1.0-SNAPSHOT</version>
   </dependency>
   ```
2. アプリ側で以下の Bean を用意します。
   - `OtpChallengeStore`（例: `JdbcOtpChallengeStore` を DataSource で生成）
   - `OtpMailer`（例: `SimpleJavaMailOtpMailer` または `LoggingOtpMailer`）
3. `application.yml` で Web設定を記述します（`/mfa` 画面、送信・検証パス、成功/失敗時のリダイレクト、保護パスのプレフィックスなど）。
4. ID/パスワード認証に成功したら `OtpWebSupport.markPasswordOk(session, userId, email)` を呼び出し、MFA画面 `/mfa` へ遷移させます。OTP検証成功後はフィルタが `MFA_OK` をセッションに入れ、保護パスにアクセスできるようになります。

### Servlet ベース（otp-web-servlet）
1. 依存に `otp-web-servlet` を追加します。
2. アプリ側で `OtpChallengeStore` と `OtpMailer` を生成し、`ServletEmailOtp2faService` に渡した上で、`OtpStartServlet` と `OtpVerifyServlet`、`MfaEnforcerFilter` をアプリの `ServletContext` に登録します。
3. セッションには `USER_ID` / `USER_EMAIL` / `PASSWORD_OK=true` をログイン成功時に必ず保存してください。MFA未完了の場合は Filter が `/mfa` にリダイレクトします。

### Servlet サンプル（otp-servlet-sample）
1. 依存を含めてビルドします。
   ```bash
   mvn -pl otp-servlet-sample -am clean package
   ```
2. Jetty でサンプルを起動します。
   ```bash
   mvn -pl otp-servlet-sample -am jetty:run
   ```
3. ブラウザで `http://localhost:8080/login` を開き、ユーザーID `user` / パスワード `password` でログインします。
4. 「確認コードを送信」を押すと OTP が生成され、コンソールログに出力されます。出力された6桁のコードを入力すると `/app` に遷移します。

### 下位モジュールの直接利用
- 独自フレームワークで使いたい場合は `otp-core`（ポリシー・検証）、`otp-storage`（永続化）、`otp-mail`（送信）を直接組み合わせて実装できます。`otp-spring-sample` や Web向けモジュールのコードが組み込み例として参考になります。

## 参考資料
- Spring Boot 向けの詳細な組み込み手順は [`otp-web-spring/README.md`](otp-web-spring/README.md) を参照してください。
- Servlet 向けの詳細な組み込み手順は [`otp-web-servlet/README.md`](otp-web-servlet/README.md) を参照してください。
- 完成形の挙動を確認したい場合は [`otp-spring-sample`](otp-spring-sample) を起動してブラウザで動作を確認してください。
