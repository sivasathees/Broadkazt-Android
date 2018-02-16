package com.nfcsnapper.nfcsnapper.api;

/**
 * Created by eivarsso on 15.05.2017.
 */

import com.nfcsnapper.nfcsnapper.model.Data;
import com.nfcsnapper.nfcsnapper.model.HerokuService;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;


public interface ApiInterface {

    @GET("account/{id}")
    Call<HerokuService> getVideos(@Path("id") String tagId);
}
