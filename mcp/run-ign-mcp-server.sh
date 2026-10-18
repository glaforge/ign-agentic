#!/usr/bin/env bash
# Copyright 2026 Google LLC
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -euo pipefail

PROJECT_DIR="/Users/glaforge/Projects/IGN"
cd "$PROJECT_DIR"

# Si le projet n'est pas encore compilé, compiler en envoyant toute sortie vers stderr (>&2)
if [[ ! -f "$PROJECT_DIR/target/mcp-classpath.txt" || ! -d "$PROJECT_DIR/target/classes" ]]; then
  "$PROJECT_DIR/mvnw" compile dependency:build-classpath -Dmdep.outputFile=target/mcp-classpath.txt -q >&2
fi

CP="$PROJECT_DIR/target/classes:$(cat "$PROJECT_DIR/target/mcp-classpath.txt")"

exec java \
  -Dign.mcp.stdio=true \
  -Dorg.slf4j.simpleLogger.logFile=System.err \
  -cp "$CP" \
  fr.ign.workshop.mcp.IgnGeoplateformeMcpServer
