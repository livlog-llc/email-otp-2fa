package jp.livlog.otp.storage;

import jp.livlog.otp.model.OtpChallenge;

import java.util.Optional;

public interface OtpChallengeStore {
    void save(OtpChallenge challenge);

    Optional<OtpChallenge> find(String challengeId);

    void delete(String challengeId);

    /**
     * 任意：ユーザー単位で「最新の未完了チャレンジ」を探したい場合に使う。
     * 実装できないストアもあるので Optional でOK。
     */
    default Optional<OtpChallenge> findLatestPendingByUser(String userId) {
        return Optional.empty();
    }
}
