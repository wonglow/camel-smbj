/**
 *  Copyright [2018] [Juraj Borza]
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

package com.github.jborza.camel.component.smbj

import com.github.jborza.camel.component.smbj.exceptions.FileAlreadyExistsException
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.main.Main
import org.apache.commons.io.FileUtils

import java.nio.charset.StandardCharsets
import java.nio.file.Paths

class FileToSmbFileExistSpecIT extends SmbSpecBase {
    static final HOST = "localhost"
    static final PORT = "4445"
    static final USER = "user"
    static final PASS = "pass"
    static final SHARE = "share"

    static final TEST_FILENAME = "file-to-smb.txt"
    static final NEW_CONTENT = "Hello, camel-smbj!"

    static final OUTPUT_DIR = "output"

    def getSmbUri() {
        return "smb2://${HOST}:${PORT}/${SHARE}?username=${USER}&password=${PASS}"
    }

    def getSambaRootDir() {
        if (isWindows())
            return "c:\\temp\\camel-smbj"
        else
            return "/tmp/camel-smbj"
    }

    def isWindows() {
        return (System.properties['os.name'].toLowerCase().contains('windows'))
    }

    def setup() {
        //clean samba target directory
        File directory = new File(getSambaRootDir())
        FileUtils.cleanDirectory(directory)
        //prepare file to copy
        File subDir = new File("to-smb")
        if (!subDir.exists())
            subDir.mkdir()
        //clean source directory
        FileUtils.cleanDirectory(subDir)
        File target = new File(Paths.get("to-smb", TEST_FILENAME).toString())
        FileUtils.writeStringToFile(target, NEW_CONTENT, StandardCharsets.UTF_8)
    }

    def "one file from file to subdirectory in smb with fileExist=Ignore option doesn't overwrite it"() {
        given:
        //prepare the file so it already exists
        File directory = new File(getSambaRootDir() + "/output")
        directory.mkdir()
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Ignore")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        //and definitely not NEW_CONTENT
        content != NEW_CONTENT
        content == originalContent
    }


    def "one file from file to subdirectory in smb with fileExist=Override option does overwrite it"() {
        given:
        //prepare the file so it already exists
        File directory = new File(getSambaRootDir() + "/output")
        directory.mkdir()
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Override")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content != originalContent
        content == NEW_CONTENT
    }

    def "file from file to smb with fileExist=Override&eagerDelete=false does overwrite"() {
        given:
        //prepare the file so it already exists
        File directory = new File(getSambaRootDir() + "/output")
        directory.mkdir()
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Override&eagerDeleteTargetFile=false&tempPrefix=smbj.")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content != originalContent
        content == NEW_CONTENT
    }

    def "file from file to smb with fileExist=Override&eagerDelete=false and existing temp target does overwrite"() {
        given:
        //prepare the file so it already exists
        File directory = new File(getSambaRootDir() + "/output")
        directory.mkdir()
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)
        //prepare the temp target as well
        File existingTempTargetFile = new File(getSambaRootDir() + "/output/smbj." + TEST_FILENAME)
        FileUtils.touch(existingTempTargetFile)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Override&eagerDeleteTargetFile=false&tempPrefix=smbj.")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content != originalContent
        content == NEW_CONTENT
        //also assert that the temporary file no longer exists
        File tempTarget = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, "smbj." + TEST_FILENAME).toString())
        !tempTarget.exists()
    }

    def "one file from file to subdirectory in smb with fileExist=Override&eagerDelete=true does overwrite"() {
        given:
        //prepare the file so it already exists
        File directory = new File(getSambaRootDir() + "/output")
        directory.mkdir()
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Override&eagerDeleteTargetFile=true")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content != originalContent
        content == NEW_CONTENT
    }

    def "one file from file to subdirectory in smb with fileExist=Fail should fail"() {
        given:
        //prepare the file so it already exists
        File directory = new File(getSambaRootDir() + "/output")
        directory.mkdir()
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        def thrownFileAlreadyExistsException = false
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                onException(FileAlreadyExistsException.class)
                        .process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        thrownFileAlreadyExistsException = true
                        camelContext.stop()
                    }
                })
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Fail")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        thrownFileAlreadyExistsException
        //original file not overwritten
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        // definitely not NEW_CONTENT
        content != NEW_CONTENT
        content == originalContent
    }

    def "fileExist=append appends to existing file"() {
        given:
        File existingFile = new File(getSambaRootDir() + "/output/" + TEST_FILENAME)
        def originalContent = "original content"
        FileUtils.writeStringToFile(existingFile, originalContent, StandardCharsets.UTF_8)

        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Append")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        //original file not overwritten
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == originalContent + NEW_CONTENT
    }

    def "fileExist=append creates file if no file exist"() {
        when:
        //set up camel context
        def main = new Main()
        def camelContext = main.getOrCreateCamelContext()
        camelContext.addRoutes(new RouteBuilder() {
            @Override
            void configure() throws Exception {
                onException(FileAlreadyExistsException.class)
                        .process(new Processor() {
                    @Override
                    void process(Exchange exchange) throws Exception {
                        camelContext.stop()
                    }
                })
                from("file://to-smb?fileName=" + TEST_FILENAME)
                        .to("smb2://localhost:4445/share/output/?username=user&password=pass&fileExist=Append")
                        .stop()
            }
        })
        camelContext.start()
        Thread.sleep(DEFAULT_CAMEL_CONTEXT_DURATION)
        camelContext.stop()

        then:
        //original file not overwritten
        File target = new File(Paths.get(getSambaRootDir(), OUTPUT_DIR, TEST_FILENAME).toString())
        target.exists()
        String content = FileUtils.readFileToString(target, StandardCharsets.UTF_8)
        content == NEW_CONTENT
    }
}