# Maven 対応の提案

JitPack 設定を撤去した上で、今後 Maven プラグイン／依存ライブラリとして配布するための方針案です。現状のビルドは Maven Release Plugin を用いた従来手順のままとし、ここでは段階的に進めるためのロードマップを示します。

## 1. 配布先リポジトリの整理
- **Maven Central（推奨）**: OSSRH（Sonatype）経由で公開する。企業や CI で追加設定なしに取得でき、プラグイン解決にも有利。
- **GitHub Packages（補助的なミラー）**: 認証が必要なため、Central 公開後にキャッシュ用途として検討する。必須ではない。

## 2. 発行に必要な POM 設定（追加予定）
1. `distributionManagement` を Central（OSSRH）向けに設定。
2. `nexus-staging-maven-plugin` でリリース／クローズ／プロモートを自動化。
3. 署名付きアーティファクトを配布するため、`maven-gpg-plugin` の有効化と CI シークレットに GPG キーを登録。
4. プラグインとして利用する場合でも Maven Central を参照できるため、`pluginRepositories` はデフォルトの中央リポジトリのみを前提にする（追加設定を強要しない）。

## 3. GitHub Actions での自動化案
- トリガー: `workflow_dispatch` と Release 作成時。
- ステップ: `mvn -N clean install` で親 POM を登録 → 署名付き `mvn -pl '!otp-servlet-sample,!otp-spring-sample' -am deploy` を実行。
- シークレット: `OSSRH_USERNAME` / `OSSRH_PASSWORD`、`GPG_PRIVATE_KEY` / `GPG_PASSPHRASE` を利用。

## 4. ローカル検証手順（中央公開前の想定）
1. 署名キーをインポート（`gpg --import`）。
2. `mvn -N clean install` で親 POM を登録。
3. サンプルを除外してビルドし、`target` 配下のアーカイブを確認。
4. `mvn -DskipTests -pl '!otp-servlet-sample,!otp-spring-sample' -am verify` で署名や依存解決の整合性を確認。

## 5. 今後のタスク
- OSSRH プロジェクト登録と `groupId` 所有者検証を完了する。
- 署名キーの管理ポリシーを決定し、CI に安全に登録する。
- 公開後、README に Maven Central のバッジと取得例を追記する。

本提案はあくまで方針案であり、実際の設定は上記手順に沿って段階的に導入してください。
