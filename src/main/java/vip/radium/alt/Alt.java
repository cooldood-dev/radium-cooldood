package vip.radium.alt;

import java.util.Objects;

public final class Alt {

    private final String email;
    private final String password;
    private String refreshToken;
    private String username;
    private boolean banned;
    private boolean working;

    public Alt(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Alt(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public Alt(String refreshToken) {
        this.email = "RefreshToken";
        this.password = "";
        this.refreshToken = refreshToken;
    }

    public boolean isRefreshToken() {
        return refreshToken != null && !refreshToken.isEmpty();
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Alt alt = (Alt) o;
        if (isRefreshToken() || alt.isRefreshToken()) {
            return Objects.equals(refreshToken, alt.refreshToken);
        }
        return Objects.equals(email, alt.email) && Objects.equals(password, alt.password);
    }

    @Override
    public int hashCode() {
        return isRefreshToken() ? Objects.hash(refreshToken) : Objects.hash(email, password);
    }

    public boolean isWorking() {
        return working;
    }

    public void setWorking(boolean working) {
        this.working = working;
    }

    public boolean isBanned() {
        return banned;
    }

    public void setBanned(boolean banned) {
        this.banned = banned;
    }

    public void setBanned() {
        setBanned(true);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }
}
