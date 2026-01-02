# ソースコードレビュー

このドキュメントはリポジトリ全体（OTPコア、ストレージ、メール送信、Servlet/Spring連携）を横断的に確認した所感です。安全なOTPハッシュ化や状態管理など重要なポイントは概ね良好でしたが、実装の一貫性・信頼性向上のために改善できる箇所をまとめました。

## 良い点
- **安全なOTPハッシュ**: PBKDF2-HMAC-SHA256 とユーザー固有のソルト、12万回のストレッチングでOTPを保存し、比較もコンスタントタイム実装を用いるなど、平文漏えいとタイミング攻撃を避ける配慮がなされています。【F:otp-core/src/main/java/jp/livlog/otp/hash/OtpHasher.java†L11-L73】
- **整然とした検証ステートマシン**: `OtpVerifier` が「チャレンジ未発行」「すでに検証済み」「試行回数超過」「有効期限切れ」を順序立てて判定し、状況に応じたステータスを返却することで、永続化層が正しい状態遷移を記録しやすくなっています。【F:otp-core/src/main/java/jp/livlog/otp/service/OtpVerifier.java†L25-L124】
- **Spring連携の送信制御**: Springのサービスは再送回数とインターバルをチェックし、必要に応じて既存チャレンジを再利用するなど、濫用防止と送信コスト削減の両立が図られています。【F:otp-web-spring/src/main/java/jp/livlog/otp/web/spring/service/SpringEmailOtp2faService.java†L83-L133】

## 改善提案
- **Servlet連携の再送制御を強化**: Servlet版はペンディング状態のチャレンジ確認や再送クールダウンを行わずに新規発行しています。Spring版と同等のポリシー（最大再送回数、再送間隔、既存チャレンジの再利用、ユーザー一致確認）を導入することで、OTPスパム防止と挙動の一貫性を高められます。【F:otp-web-servlet/src/main/java/jp/livlog/otp/web/servlet/service/ServletEmailOtp2faService.java†L48-L90】【F:otp-web-spring/src/main/java/jp/livlog/otp/web/spring/service/SpringEmailOtp2faService.java†L83-L142】
- **永続化の排他・原子性**: JDBCストアの `save` は UPDATE 後に INSERT を行うだけでトランザクション境界も楽観ロックもなく、並行リクエストで状態がロールバックしたり重複行が残るリスクがあります。DBのアップサート構文＋バージョンカラム、またはトランザクションで更新と挿入をまとめて原子化することを検討してください。【F:otp-storage/src/main/java/jp/livlog/otp/storage/jdbc/JdbcOtpChallengeStore.java†L18-L80】
- **期限切れデータの整理と索引**: JDBCストアは期限切れ・ロック済みチャレンジを削除せず、`last_sent_at` の降順取得もインデックス前提が明示されていません。TTL付与や定期クリーンアップ、`user_id`・`status`・`last_sent_at` へのインデックスを追加して、データ肥大や取得遅延を防ぐとよいでしょう。【F:otp-storage/src/main/java/jp/livlog/otp/storage/jdbc/JdbcOtpChallengeStore.java†L82-L117】
- **運用監視の勘所**: ログにはチャレンジIDやユーザーIDを含めた情報が少なく、再送回数やロックアウト判定の可観測性が限定的です。送信・検証結果のログにユーザー識別子（PIIには注意）や状態遷移を付加し、メトリクス送出を行うと、異常検知やサポート対応が容易になります。

## まとめ
全体としてセキュリティ面はよく考慮されています。上記の再送ポリシー統一、永続化の原子性確保、データライフサイクル管理、運用監視の補強を行うことで、プロダクション運用時の堅牢性と一貫性が高まり、複数エントリーポイント（Servlet/Spring）間で同様のユーザー体験を提供できるはずです。
