# JitPack でのマルチモジュール配布について

JitPack はリポジトリ単位でビルドを実行しますが、Maven のマルチモジュール構成でも各子 POM の成果物を個別に取得できます。ここでは、親 POM への依存を避けたい場合に、子モジュールを JitPack から直接利用・公開する際の考え方をまとめます。

## 前提
- JitPack の `groupId` は `com.github.<GitHubユーザー名>` となります。
- `version` は Git タグ（例: `v0.1.0`）かコミットハッシュを指定します。
- `artifactId` は子モジュールの POM で定義したもの（例: `otp-core`）を使います。

## 各子モジュールを直接利用する例
親 POM をプロジェクトに組み込まなくても、子モジュールを個別に依存追加できます。

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.github.<GitHubユーザー名></groupId>
    <artifactId>otp-core</artifactId>
    <version>v0.1.0</version>
  </dependency>
  <dependency>
    <groupId>com.github.<GitHubユーザー名></groupId>
    <artifactId>otp-mail</artifactId>
    <version>v0.1.0</version>
  </dependency>
</dependencies>
```

### Maven プラグインとして配布したい場合
`packaging` が `maven-plugin` のモジュールを持っていれば、`pluginRepositories` に JitPack を追加し、同じ座標で解決できます。

```xml
<pluginRepositories>
  <pluginRepository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </pluginRepository>
</pluginRepositories>

<build>
  <plugins>
    <plugin>
      <groupId>com.github.<GitHubユーザー名></groupId>
      <artifactId>otp-build-plugin</artifactId>
      <version>v0.1.0</version>
    </plugin>
  </plugins>
</build>
```

## 子モジュールだけをビルドしたい場合の `jitpack.yml`
親 POM のインストールを避けたい、または特定モジュールだけを公開したい場合は、`jitpack.yml` でビルド対象を限定できます。このリポジトリでは、実際に次の設定を用いて `otp-core` / `otp-mail` / `otp-storage` / `otp-web-servlet` / `otp-web-spring` のみをビルドしています。

```yaml
jdk:
  - openjdk17
install:
  - mvn -pl otp-core,otp-mail,otp-storage,otp-web-servlet,otp-web-spring -am -DskipTests install
```

- `-pl` で JitPack がビルドすべきモジュールを指定します。
- `-am` を付けると依存するモジュール（たとえば `otp-core`）も一緒にビルドされます。
- `otp-spring-sample` など配布不要なサンプルは含めないことで、不要な依存を避けられます。

## 親 POM を避けたい場合の代替策
- **単一リポジトリで続ける場合**: 上記のように `jitpack.yml` でビルド対象を限定し、必要な子モジュールだけを解決する。
- **モジュールごとにリポジトリを分割する**: 各子モジュールを独立した GitHub リポジトリに切り出せば、JitPack の `artifactId` をシンプルにしつつ、親 POM への依存を完全になくせます。

## 注意点
- JitPack は Git タグごとにビルドするため、タグを付ける際に必ず子モジュールのバージョンを更新してください。
- サンプルアプリなど、公開したくないモジュールは `jitpack.yml` で除外しないと、ビルドに失敗する場合があります。
- CI での自動テストや署名はサポートされないため、正式配布は Maven Central など別経路も検討してください。
