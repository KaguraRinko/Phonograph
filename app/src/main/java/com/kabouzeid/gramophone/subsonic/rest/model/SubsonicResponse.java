package com.kabouzeid.gramophone.subsonic.rest.model;

public class SubsonicResponse {
    public String status;
    public String version;
    public SubsonicError error;
    public SubsonicArtists artists;
    public SubsonicArtist artist;
    public SubsonicAlbum album;
    public SubsonicGenres genres;
    public SubsonicPlaylists playlists;
    public SubsonicPlaylist playlist;

    public boolean isOk() {
        return "ok".equals(status);
    }
}
