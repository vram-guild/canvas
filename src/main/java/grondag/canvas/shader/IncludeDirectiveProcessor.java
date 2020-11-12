/*
 * Copyright 2019, 2020 grondag
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.  You may obtain a copy
 * of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package grondag.canvas.shader;

import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IncludeDirectiveProcessor {

	private static final Pattern PATTERN = Pattern.compile("^#include\\s+\"?([\\w]+:[\\w/.]+)\"?", Pattern.MULTILINE);

	public static String process(Function<Identifier, String> resourceLoader, Identifier identifier) {
		return process0(resourceLoader, identifier, new HashSet<>());
	}

	private static String process0(Function<Identifier, String> resourceLoader, Identifier identifier, Set<Identifier> included) {
		if (included.add(identifier)) {
			return replaceAll(PATTERN.matcher(resourceLoader.apply(identifier)), id -> process0(resourceLoader, new Identifier(id.group(1)), included));
		}

		return "";
	}

	private static String replaceAll(Matcher matcher, Function<MatchResult, String> replacer) {
		matcher.reset();
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String replacement = replacer.apply(matcher);
			matcher.appendReplacement(sb, replacement);
		}

		matcher.appendTail(sb);
		return sb.toString();
	}
}
