/*
  Copyright (c) 2023, Oracle and/or its affiliates.

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

     https://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

export const JDK_RELEASES_TRACK_URL = `https://www.java.com/releases/releases.json`;

export const ORACLE_JDK_BASE_DOWNLOAD_URL = `https://download.oracle.com/java/`;

export const ORACLE_JDK_VERSION_FALLBACK_DOWNLOAD_VERSIONS = {
  latestVersion :{
    version: '21.0.2',
    family: '21'
  },
  latestLtsVersion :{
    version: '17.0.10',
    family: '17'
  },
};

export const OPEN_JDK_VERSION_DOWNLOAD_LINKS: { [key: string]: string } = {
  "21.0.2": "https://download.java.net/java/GA/jdk21.0.2/f2283984656d49d69e91c558476027ac/13/GPL/openjdk-21.0.2"
};