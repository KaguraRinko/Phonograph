package com.kabouzeid.gramophone.subsonic.rest;

import com.kabouzeid.gramophone.subsonic.rest.model.SubsonicResponseEnvelope;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

public interface SubsonicApiService {
    @GET("ping.view")
    Call<SubsonicResponseEnvelope> ping(@QueryMap Map<String, String> auth);

    @GET("getArtists.view")
    Call<SubsonicResponseEnvelope> getArtists(@QueryMap Map<String, String> auth);

    @GET("getArtist.view")
    Call<SubsonicResponseEnvelope> getArtist(@QueryMap Map<String, String> auth, @Query("id") String id);

    @GET("getAlbum.view")
    Call<SubsonicResponseEnvelope> getAlbum(@QueryMap Map<String, String> auth, @Query("id") String id);

    @GET("getGenres.view")
    Call<SubsonicResponseEnvelope> getGenres(@QueryMap Map<String, String> auth);

    @GET("getPlaylists.view")
    Call<SubsonicResponseEnvelope> getPlaylists(@QueryMap Map<String, String> auth);

    @GET("getPlaylist.view")
    Call<SubsonicResponseEnvelope> getPlaylist(@QueryMap Map<String, String> auth, @Query("id") String id);
}
