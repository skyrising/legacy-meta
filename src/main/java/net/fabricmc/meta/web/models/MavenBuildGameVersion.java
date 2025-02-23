/*
 * Copyright (c) 2021 Legacy Fabric/Quilt
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

package net.fabricmc.meta.web.models;

import net.fabricmc.meta.utils.YarnVersionParser;

public class MavenBuildGameVersion extends MavenBuildVersion {

	String gameVersion;

	public MavenBuildGameVersion(String base, String maven) {
		super(base, maven);
		gameVersion = new YarnVersionParser(maven.split(":")[2]).getMinecraftVersion();

	}

	public String getGameVersion() {
		return gameVersion;
	}

	@Override
	public boolean test(String s) {
		return getGameVersion().equals(s);
	}
}
