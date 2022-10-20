package org.anefdev.flowtherockapp.service;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.anefdev.flowtherockapp.model.MusicPlaylist;
import org.anefdev.flowtherockapp.model.MusicTrack;
import org.anefdev.flowtherockapp.model.SpotifyUser;
import org.anefdev.flowtherockapp.util.PlaylistParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.enums.AuthorizationScope;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.model_objects.miscellaneous.AudioAnalysis;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetAudioAnalysisForTrackRequest;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;
import se.michaelthelin.spotify.requests.data.users_profile.GetCurrentUsersProfileRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@NoArgsConstructor
public class FlowTheRockService {
    @Value("${spotify-client-id}")
    private String CLIENT_ID;
    @Value("${spotify-client-secret}")
    private String CLIENT_SECRET;
    @Value("${spotify-callback-uri}")
    private String CALLBACK_URL;
    private SpotifyApi spotifyApi;
    private List<MusicPlaylist> allPlaylists;
    private List<MusicTrack> playlistParsed;
    private SpotifyUser currentUser;
    private static final Logger LOGGER = LoggerFactory.getLogger(FlowTheRockService.class);

    /**
     * Gets URI for authorization with Spotify.
     * <br>
     * Sync
     * @return Authorization code URI
     */
    public URI getAuthorisationCodeURI() {

        LOGGER.info("getAuthorisationCodeURI [ Get authorization code URI ... ]");
        final URI CALLBACK_URI = SpotifyHttpManager.makeUri(CALLBACK_URL);

        this.spotifyApi = new SpotifyApi.Builder()
                .setClientId(CLIENT_ID)
                .setClientSecret(CLIENT_SECRET)
                .setRedirectUri(CALLBACK_URI)
                .build();

        final AuthorizationCodeUriRequest authorizationCodeUriRequest =
                spotifyApi.authorizationCodeUri()
                .scope(AuthorizationScope.USER_LIBRARY_READ,AuthorizationScope.USER_READ_PRIVATE, AuthorizationScope.USER_READ_EMAIL)
                .show_dialog(true)
                .build();
        var uri = authorizationCodeUriRequest.execute();
        LOGGER.info("getAuthorisationCodeURI [ URI: " + uri.toString() + " ]");
        LOGGER.info("getAuthorisationCodeURI [ OK ]");
        return uri;

    }

    /**
     * Gets and saves the access and refresh tokens in SpotifyApi object.
     * <br>
     * Sync
     * @param code Authorization code for Spotify
     */
    @SneakyThrows
    public void setAuthorizationToken(String code) {

        LOGGER.info("setAuthorizationToken [ Get authorization token ...");
        LOGGER.info("setAuthorizationToken [ Authorization code: " + code + " ]");

        final AuthorizationCodeRequest authorizationCodeRequest = spotifyApi.authorizationCode(code).build();
        final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
        // Set access and refresh token for further "spotifyApi" object usage
        spotifyApi.setAccessToken(authorizationCodeCredentials.getAccessToken());
        spotifyApi.setRefreshToken(authorizationCodeCredentials.getRefreshToken());

        LOGGER.info("setAuthorizationToken [ Access token: " + spotifyApi.getAccessToken() + " ]");
        LOGGER.info("setAuthorizationToken [ Refresh token: " + spotifyApi.getRefreshToken() + " ]");

    }

    /**
     * Loads all playlists of the current user.
     * <br>
     * Sync
     * @return List of user's playlists
     */
    @SneakyThrows
    public List<MusicPlaylist> loadAllUsersPlaylists() {

        LOGGER.info("loadAllUsersPlaylists [ Loading all user's playlists ... ]");
        List<MusicPlaylist> allUserPlaylists;

        final GetListOfUsersPlaylistsRequest getListOfUsersPlaylistsRequest = spotifyApi
                .getListOfUsersPlaylists(this.currentUser.getId())
                .build();
        final Paging<PlaylistSimplified> playlistSimplifiedPaging = getListOfUsersPlaylistsRequest.execute();
        final List<PlaylistSimplified>  allUserPlaylistsSimple = Arrays.stream(playlistSimplifiedPaging.getItems()).toList();
        allUserPlaylists = PlaylistParser.getAllPlaylists(allUserPlaylistsSimple);

        this.allPlaylists = allUserPlaylists;
        LOGGER.info("loadAllUsersPlaylists [ Playlists count: " + this.allPlaylists.size() + " ]");
        LOGGER.info("loadAllUsersPlaylists [ OK ]");

        return this.allPlaylists;
    }

    /**
     * Loads playlist by passed ID.
     * <br>
     * Async
     * @param playlistId ID of the playlist
     * @return List of MusicTrack playlist
     */
    @SneakyThrows
    public List<MusicTrack> loadPlaylist(String playlistId) {

        LOGGER.info("loadPlaylist [ Loading playlist ... ]");
        LOGGER.info("loadPlaylist [ Playlist with ID: " + playlistId + " ]");

        Paging<PlaylistTrack> playlist;

        playlist = spotifyApi.getPlaylistsItems(playlistId)
                    .build()
                    .execute();

        assert playlist != null;
        LOGGER.info("loadPlaylist [ Loading playlist with size: " + playlist.getTotal() + " ]");

        var playlistFinal = new ArrayList<>(Arrays.stream(playlist.getItems()).toList());
        while (playlist.getTotal() > playlistFinal.size()) {
            var playlistPagination = spotifyApi.getPlaylistsItems(playlistId)
                    .offset(playlistFinal.size())
                    .build()
                    .execute();
            playlistFinal.addAll(Arrays.stream(playlistPagination.getItems()).toList());
        }

        LOGGER.info("loadPlaylist [ Extracting track-IDs ... ]");
        var playlistNoMetaData = PlaylistParser.getTrackListNoMetaData(playlistFinal);



        LOGGER.info("loadPlaylist [ Loading tracks-data ... ]");
        var tracksAnalyzed = playlistNoMetaData.stream().map(
                track -> {
                    Track trackInfo = this.loadTrackInfo(track.getId());
                    MusicTrack trackAnalysis = this.loadTrackAnalysis(track.getId());
                    track =  PlaylistParser.parseTrackInfo(trackAnalysis, trackInfo);
                    return PlaylistParser.parseTrackAnalysisKey(track);
                }
        ).toList();
        this.playlistParsed = tracksAnalyzed;
        LOGGER.info("loadPlaylist [ OK ]");
        return this.playlistParsed;
    }

    /**
     * Loads song info by passed track ID.
     * <br>
     * Async
     * @param trackId ID of the track
     * @return Track song info
     */
    @SneakyThrows
    private Track loadTrackInfo(String trackId) {
        LOGGER.info("loadTrackInfo [ Loading song info ... ]");
        final GetTrackRequest getTrackRequest = spotifyApi.getTrack(trackId).build();
        final CompletableFuture<Track> trackFuture = getTrackRequest.executeAsync();
        final Track track = trackFuture.join();
        LOGGER.info("loadTrackInfo [ Song: " + track.toString() + " ]");
        LOGGER.info("loadTrackInfo [ OK ]");
        return track;
    }

    /**
     * Loads audio analysis by passed track ID.
     * <br>
     * Async
     * @param trackId ID of the track
     * @return MusicTrack audio analysis
     */
    @SneakyThrows
    private MusicTrack loadTrackAnalysis(String trackId) {
        LOGGER.info("loadTrackAnalysis [ Loading audio analysis ... ]");
        final GetAudioAnalysisForTrackRequest getTrackAnalysisRequest = spotifyApi.getAudioAnalysisForTrack(trackId).build();
        final CompletableFuture<AudioAnalysis> audioAnalysisFuture = getTrackAnalysisRequest.executeAsync();
        final AudioAnalysis audioAnalysis = audioAnalysisFuture.join();
        LOGGER.info("loadTrackAnalysis [ Audio analysis: " + audioAnalysis.toString() + " ]");
        LOGGER.info("loadTrackAnalysis [ OK ]");
        return PlaylistParser.convertTrackAnalysisToTrack(audioAnalysis);
    }

    /**
     * Sorts current playlist via Camelot wheel by passed track id.
     * <br>
     * Sync
     * @param trackId String id of current song
     * @return sorted playlist
     */
    public List<MusicTrack> sortPlaylist(String trackId) {

        LOGGER.info("sortPlaylist [ Sorting playlist ... ]");
        LOGGER.info("sortPlaylist [ Removing all matched tags ... ]");

        for (MusicTrack track: this.playlistParsed) {
            track.setMatched(false);
        }

        LOGGER.info("sortPlaylist [ Sorting playlist ... ]");
        LOGGER.info("sortPlaylist [ OK ]");

        return PlaylistParser.sortPlaylist(this.playlistParsed, trackId);
    }

    /**
     * Loads curren Spotify user's data
     * <br>
     * Sync
     * @return current user's data
     */
    @SneakyThrows
    public SpotifyUser loadUserData() {

        LOGGER.info("loadUserData [ Loading user data ...]");

        if (this.currentUser == null) {
            final GetCurrentUsersProfileRequest getCurrentUsersProfileRequest = spotifyApi
                    .getCurrentUsersProfile()
                    .build();

            final User user = getCurrentUsersProfileRequest.execute();
            LOGGER.info("loadUserData []");
            this.currentUser = new SpotifyUser(
                    user.getId(),
                    user.getEmail(),
                    user.getDisplayName(),
                    user.getImages()[0].getUrl(),
                    user.getUri());
        }

        LOGGER.info("loadUserData [ Current user: " + this.currentUser + "]");
        LOGGER.info("loadUserData [ OK ]");

        return this.currentUser;
    }
}
