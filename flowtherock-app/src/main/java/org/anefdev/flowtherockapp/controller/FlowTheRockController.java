package org.anefdev.flowtherockapp.controller;

import lombok.SneakyThrows;
import org.anefdev.flowtherockapp.model.MusicPlaylist;
import org.anefdev.flowtherockapp.model.MusicTrack;
import org.anefdev.flowtherockapp.model.SpotifyUser;
import org.anefdev.flowtherockapp.service.FlowTheRockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

@RestController
@RequestMapping(path = "flowtherock/api")
@CrossOrigin("*")
public class FlowTheRockController {

    private static Logger LOGGER = LoggerFactory.getLogger(FlowTheRockController.class);
    private static final String REDIRECT_WEB_CLIENT = "http://164.90.185.125/index.html";
    @Autowired
    FlowTheRockService service;

    @GetMapping(path = "/authorize")
    @SneakyThrows
    public ModelAndView authorize() {
        LOGGER.info("New login with Spotify ...");
        return new ModelAndView("redirect:" + this.service.getAuthorisationCodeURI().toString());
    }

    @GetMapping(path = "/callback")
    public ModelAndView callback(@RequestParam(value = "code") final String code) {
        LOGGER.info("Get authorization token ...");
        this.service.setAuthorizationToken(code);
        return new ModelAndView("redirect:"  + REDIRECT_WEB_CLIENT);
    }

    @GetMapping(path = "/user")
    public SpotifyUser getUserData() {
        LOGGER.info("Get user data ...");
        return this.service.loadUserData();
    }

    @GetMapping(path = "/playlists")
    public List<MusicPlaylist> getPlaylists() {
        return this.service.loadAllUsersPlaylists();
    }

    @GetMapping(path = "/playlist/load")
    @SneakyThrows
    public List<MusicTrack> loadPlaylist(@RequestParam(value = "playlistId") String playlistId) {
        return service.loadPlaylist(playlistId);
    }

    @GetMapping(path = "/playlist/sort")
    @SneakyThrows
    public List<MusicTrack> sort(@RequestParam(value = "trackId") String trackId) {
        return service.sortPlaylist(trackId);
    }

}
