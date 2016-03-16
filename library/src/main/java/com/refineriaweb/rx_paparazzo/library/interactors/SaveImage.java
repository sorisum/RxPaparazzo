/*
 * Copyright 2016 Refinería Web
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

package com.refineriaweb.rx_paparazzo.library.interactors;

import android.net.Uri;

import javax.inject.Inject;

import rx.Observable;

public final class SaveImage extends UseCase<String> {
    private final GetPath getPath;
    private Uri uri;

    @Inject public SaveImage(GetPath getPath) {
        this.getPath = getPath;
    }

    public SaveImage with(Uri uri) {
        this.uri = uri;
        return this;
    }

    @Override public Observable<String> react() {
        return getPath.with(uri).react();
    }

}
