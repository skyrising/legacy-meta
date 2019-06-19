/*
 * Copyright (c) 2019 FabricMC
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

package net.fabricmc.meta.web;

import io.javalin.Context;
import net.fabricmc.meta.FabricMeta;
import net.fabricmc.meta.web.models.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@SuppressWarnings("Duplicates")
public class EndpointsV2 {

	public static void setup() {

		WebServer.jsonGet("/v2/versions", () -> FabricMeta.database);

		WebServer.jsonGet("/v2/versions/game", () -> FabricMeta.database.game);
		WebServer.jsonGet("/v2/versions/game/yarn", () -> compatibleGameVersions(FabricMeta.database.mappings, MavenBuildGameVersion::getGameVersion, v -> new BaseVersion(v.getGameVersion(), v.isStable())));
		WebServer.jsonGet("/v2/versions/game/intermediary", () -> compatibleGameVersions(FabricMeta.database.intermediary, BaseVersion::getVersion, v -> new BaseVersion(v.getVersion(), v.isStable())));

		WebServer.jsonGet("/v2/versions/yarn", () -> FabricMeta.database.mappings);
		WebServer.jsonGet("/v2/versions/yarn/:game_version", context -> filter(context, FabricMeta.database.mappings));

		WebServer.jsonGet("/v2/versions/intermediary", () -> FabricMeta.database.intermediary);
		WebServer.jsonGet("/v2/versions/intermediary/:game_version", context -> filter(context, FabricMeta.database.intermediary));

		WebServer.jsonGet("/v2/versions/loader", () -> FabricMeta.database.loader);
		WebServer.jsonGet("/v2/versions/loader/:game_version", EndpointsV2::getLoaderInfoAll);
		WebServer.jsonGet("/v2/versions/loader/:game_version/:loader_version", EndpointsV2::getLoaderInfo);

	}

	private static <T extends Predicate<String>> List filter(Context context, List<T> versionList) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return Collections.emptyList();
		}
		return versionList.stream().filter(t -> t.test(context.pathParam("game_version"))).collect(Collectors.toList());

	}

	private static Object getLoaderInfo(Context context) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}
		if (!context.pathParamMap().containsKey("loader_version")) {
			return null;
		}

		String gameVersion = context.pathParam("game_version");
		String loaderVersion = context.pathParam("loader_version");

		MavenBuildVersion loader = FabricMeta.database.loader.stream()
			.filter(mavenBuildVersion -> loaderVersion.equals(mavenBuildVersion.getVersion()))
			.findFirst().orElse(null);

		MavenVersion mappings = FabricMeta.database.intermediary.stream()
			.filter(t -> t.test(gameVersion))
			.findFirst().orElse(null);

		if (loader == null) {
			context.status(400);
			return "no loader version found for " + gameVersion;
		}
		if (mappings == null) {
			context.status(400);
			return "no mappings version found for " + gameVersion;
		}
		return new LoaderInfoV2(loader, mappings).populateMeta();
	}

	private static Object getLoaderInfoAll(Context context) {
		if (!context.pathParamMap().containsKey("game_version")) {
			return null;
		}
		String gameVersion = context.pathParam("game_version");

		MavenVersion mappings = FabricMeta.database.intermediary.stream()
			.filter(t -> t.test(gameVersion))
			.findFirst().orElse(null);

		if(mappings == null){
			return Collections.emptyList();
		}

		List<LoaderInfoV2> infoList = new ArrayList<>();

		for(MavenBuildVersion loader : FabricMeta.database.loader){
			infoList.add(new LoaderInfoV2(loader, mappings).populateMeta());
		}
		return infoList;
	}

	private static <T extends BaseVersion> List<BaseVersion> compatibleGameVersions(List<T> list, Function<T, String> gameVersionSupplier, Function<T, BaseVersion> baseVersionSupplier){
		List<BaseVersion> versions = new ArrayList<>();
		Predicate<String> contains = s -> versions.stream().anyMatch(baseVersion -> baseVersion.getVersion().equals(s));

		for(T entry : list){
			if (!contains.test(gameVersionSupplier.apply(entry))){
				versions.add(baseVersionSupplier.apply(entry));
			}
		}

		return versions;
	}



}
