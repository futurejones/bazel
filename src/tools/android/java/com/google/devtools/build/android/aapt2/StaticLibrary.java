// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.android.aapt2;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import javax.annotation.Nullable;

/** A static library generated by aapt2. */
public class StaticLibrary {

  private final Path library;
  private final Optional<Path> rTxt;
  private final Optional<List<Path>> assets;
  private final Optional<Path> sourceJar;

  private StaticLibrary(
      Path library, Optional<Path> rTxt, Optional<List<Path>> assets, Optional<Path> sourceJar) {
    this.library = library;
    this.rTxt = rTxt;
    this.assets = assets;
    this.sourceJar = sourceJar;
  }

  public static StaticLibrary from(Path library) {
    return of(library, Optional.empty(), Optional.empty(), Optional.empty());
  }

  public static StaticLibrary from(Path library, Path rTxt) {
    return of(library, Optional.of(rTxt), Optional.empty(), Optional.empty());
  }

  private static StaticLibrary of(
      Path library, Optional<Path> rTxt, Optional<List<Path>> assets, Optional<Path> sourceJar) {
    return new StaticLibrary(library, rTxt, assets, sourceJar);
  }

  public static StaticLibrary from(
      Path library, Path rTxt, ImmutableList<Path> assetDirs) {
    return of(
        library,
        Optional.ofNullable(rTxt),
        Optional.ofNullable(assetDirs),
        Optional.empty());
  }

  public static StaticLibrary from(
      Path library, Path rTxt, ImmutableList<Path> assetDirs, Path sourceJar) {
    return of(
        library,
        Optional.ofNullable(rTxt),
        Optional.ofNullable(assetDirs),
        Optional.ofNullable(sourceJar));
  }

  public static Collection<String> toPathStrings(List<StaticLibrary> libraries) {
    return Lists.transform(libraries, StaticLibrary::asLibraryPathString);
  }

  public StaticLibrary copyLibraryTo(Path target) throws IOException {
    return of(Files.copy(library, target), rTxt, assets, sourceJar);
  }

  public StaticLibrary copyRTxtTo(@Nullable final Path target) {
    return of(library, rTxt.map(copyTo(target)), assets, sourceJar);
  }

  public StaticLibrary copySourceJarTo(@Nullable final Path target) {
    return of(library, rTxt, assets, sourceJar.map(copyTo(target)));
  }

  private Function<Path, Path> copyTo(Path target) {
    return input -> {
      if (target == null) {
        return input;
      }
      try {
        return Files.copy(input, target);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    };
  }

  @VisibleForTesting
  public String asLibraryPathString() {
    return fixForAapt2(library).toString();
  }

  // TODO(b/64140845): aapt2 doesn't recognize .ap_ as a valid library.
  private static Path fixForAapt2(Path library) {
    if (library.getFileName().toString().endsWith(".ap_")) {
      Path fixed = library.resolveSibling(library.getFileName().toString().replace(".ap_", ".apk"));
      // The file has already been "fixed"
      if (Files.exists(fixed)) {
        return fixed;
      }
      try {

        return Files.copy(library, fixed);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return library;
  }

  private List<Path> asAssetPathStrings() {
    return assets.orElse(ImmutableList.of());
  }

  public static Collection<String> toAssetPaths(List<StaticLibrary> libraries) {
    return libraries
        .stream()
        .map(StaticLibrary::asAssetPathStrings)
        .flatMap(List::stream)
        .map(Object::toString)
        .collect(toImmutableList());
  }
}
