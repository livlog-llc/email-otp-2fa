# JitPack で各子モジュールを個別配布する手順

`otp-core` / `otp-mail` / `otp-storage` / `otp-web-servlet` / `otp-web-spring` を JitPack から配布するための手順をまとめます。親 POM を利用せずに、必要なモジュールだけを取得することを前提としています。

## 事前準備
- Java 17 を使えるローカル環境を用意します。
- `jitpack.yml` でビルド対象を上記 5 モジュールに限定しています。不要なサンプル (`otp-spring-sample` / `otp-servlet-sample`) はビルドされません。
- `pom.xml` のバージョンを更新してコミットしておくと、付けるタグと生成されるアーティファクトの対応が分かりやすくなります。

## リリース手順
1. ローカルでビルドを確認します。
   ```bash
   mvn -DskipTests -pl otp-core,otp-mail,otp-storage,otp-web-servlet,otp-web-spring -am clean install
   ```
2. リリース用の Git タグを作成して push します（例: `v0.1.8`）。
   ```bash
   git tag v0.1.8
   git push origin v0.1.8
   ```
3. JitPack のビルドを確認します。
   - https://jitpack.io/#<GitHubユーザー名>/email-otp-2fa にアクセスし、作成したタグのビルドを開始します。
   - ビルドログに `otp-core` など 5 モジュールの `install` が含まれていることを確認します。
4. 利用者向けに必要な座標を共有します。
   - 依存追加例（`groupId` は `com.github.<GitHubユーザー名>`、`version` は付けたタグ）
     ```xml
     <dependency>
       <groupId>com.github.<GitHubユーザー名></groupId>
       <artifactId>otp-core</artifactId>
       <version>v0.1.8</version>
     </dependency>
     ```
   - プラグインとして配布する場合は `pluginRepositories` に JitPack を登録し、同じ座標を指定します。

## トラブルシュート
- **サンプルの依存で失敗する**: `jitpack.yml` が有効か確認し、ビルドログに `-pl otp-core,...` が含まれているかを確認してください。
- **特定モジュールだけ更新したい**: 新しいタグを付けると、指定した 5 モジュールがまとめてビルドされます。個別タグを分けたい場合は、ブランチを分けるか別リポジトリ化を検討してください。
- **GitHub ユーザー名が変わった**: 依存の `groupId` も `com.github.<新しいユーザー名>` に合わせて変更してください。
