/*
 * Copyright 2016 Miguel Garcia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miguelbcr.ui.rx_paparazzo2.workers;

import android.Manifest;
import android.app.Activity;
import android.net.Uri;

import com.miguelbcr.ui.rx_paparazzo2.entities.Config;
import com.miguelbcr.ui.rx_paparazzo2.entities.Ignore;
import com.miguelbcr.ui.rx_paparazzo2.entities.Response;
import com.miguelbcr.ui.rx_paparazzo2.entities.TargetUi;
import com.miguelbcr.ui.rx_paparazzo2.interactors.CropImage;
import com.miguelbcr.ui.rx_paparazzo2.interactors.GetPath;
import com.miguelbcr.ui.rx_paparazzo2.interactors.GrantPermissions;
import com.miguelbcr.ui.rx_paparazzo2.interactors.PickFile;
import com.miguelbcr.ui.rx_paparazzo2.interactors.PickFiles;
import com.miguelbcr.ui.rx_paparazzo2.interactors.SaveImage;
import com.miguelbcr.ui.rx_paparazzo2.interactors.StartIntent;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Function;

public final class Files extends Worker {
  private final GrantPermissions grantPermissions;
  private final StartIntent startIntent;
  private final GetPath getPath;
  private final CropImage cropImage;
  private final SaveImage saveImage;
  private final TargetUi targetUi;
  private final Config config;

  public Files(GrantPermissions grantPermissions, StartIntent startIntent, GetPath getPath,
               CropImage cropImage, SaveImage saveImage, TargetUi targetUi, Config config) {
    super(targetUi);
    this.grantPermissions = grantPermissions;
    this.startIntent = startIntent;
    this.getPath = getPath;
    this.cropImage = cropImage;
    this.saveImage = saveImage;
    this.targetUi = targetUi;
    this.config = config;
  }

  public <T> Observable<Response<T, String>> pickFile() {
    PickFile pickFile = new PickFile(startIntent, getPath);

    return pickFile(pickFile);
  }

  public <T> Observable<Response<T, String>> pickFile(String mimeType, boolean openableOnly) {
    PickFile pickFile = new PickFile(mimeType, startIntent, getPath, openableOnly);

    return pickFile(pickFile);
  }

  public <T> Observable<Response<T, List<String>>> pickFiles() {
    PickFiles pickFiles = new PickFiles(startIntent);

    return pickFiles(pickFiles);
  }

  public <T> Observable<Response<T, List<String>>> pickFiles(String mimeType, boolean openableOnly) {
    PickFiles pickFiles = new PickFiles(mimeType, startIntent, openableOnly);

    return pickFiles(pickFiles);
  }

  public <T> Observable<Response<T, String>> pickFile(final PickFile pickFile) {
    return grantPermissions.with(permissions())
        .react()
        .flatMap(new Function<Ignore, ObservableSource<Uri>>() {
          @Override public ObservableSource<Uri> apply(Ignore ignore) throws Exception {
            return pickFile.react();
          }
        })
        .flatMap(new Function<Uri, ObservableSource<Uri>>() {
          @Override public ObservableSource<Uri> apply(Uri uri) throws Exception {
            return cropImage.with(uri).react();
          }
        })
        .flatMap(new Function<Uri, ObservableSource<String>>() {
          @Override public ObservableSource<String> apply(Uri uri) throws Exception {
            return saveImage.with(uri).react();
          }
        })
        .map(new Function<String, Response<T, String>>() {
          @Override public Response<T, String> apply(String path) throws Exception {
            return new Response<>((T) targetUi.ui(), path, Activity.RESULT_OK);
          }
        })
        .compose(this.<Response<T, String>>applyOnError());
  }

  public <T> Observable<Response<T, List<String>>> pickFiles(final PickFiles pickFiles) {
    return grantPermissions.with(permissions())
        .react()
        .flatMap(new Function<Ignore, ObservableSource<List<Uri>>>() {
          @Override public ObservableSource<List<Uri>> apply(Ignore ignore) throws Exception {
            return pickFiles.react();
          }
        })
        .flatMapIterable(new Function<List<Uri>, Iterable<Uri>>() {
          @Override public Iterable<Uri> apply(List<Uri> uris) throws Exception {
            return uris;
          }
        })
        .concatMap(new Function<Uri, ObservableSource<Uri>>() {
          @Override public ObservableSource<Uri> apply(Uri uri) throws Exception {
            return cropImage.with(uri).react();
          }
        })
        .concatMap(new Function<Uri, ObservableSource<String>>() {
          @Override public ObservableSource<String> apply(Uri uri) throws Exception {
            return saveImage.with(uri).react();
          }
        })
        .toList()
        .toObservable()
        .map(new Function<List<String>, Response<T, List<String>>>() {
          @Override public Response<T, List<String>> apply(List<String> paths) throws Exception {
            return new Response<>((T) targetUi.ui(), paths, Activity.RESULT_OK);
          }
        })
        .compose(this.<Response<T, List<String>>>applyOnError());
  }

  private String[] permissions() {
    if (config.useInternalStorage()) {
      return new String[] { Manifest.permission.READ_EXTERNAL_STORAGE };
    } else {
      return new String[] {
          Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
      };
    }
  }
}
