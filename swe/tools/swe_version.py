#!/usr/bin/env python
#
# Copyright (c) 2015,2016 The Linux Foundation. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#    * Redistributions of source code must retain the above copyright
#      notice, this list of conditions and the following disclaimer.
#    * Redistributions in binary form must reproduce the above
#      copyright notice, this list of conditions and the following
#      disclaimer in the documentation and/or other materials provided
#      with the distribution.
#    * Neither the name of The Linux Foundation nor the names of its
#      contributors may be used to endorse or promote products derived
#      from this software without specific prior written permission.
#
# THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
# WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
# MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
# ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
# BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
# CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
# SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
# BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
# WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
# OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
# IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#

import os
import sys
import re
import subprocess
import argparse
from datetime import datetime, timedelta
import configurations

# Template strings
content_template = """MAJOR={MAJOR}
MINOR={MINOR}
BUILD={BUILD}
PATCH={PATCH}
SWE_MAJOR={MAJOR}
SWE_MINOR={MINOR}
SWE_BUILD={BUILD}
SWE_PATCH={BUILDID}
LASTSWECHANGE={LASTSWECHANGE}
LASTCHANGE={LASTCHANGE}
CHANNEL={CHANNEL}
SWE_BUILD_DATE={SWE_BUILD_DATE}
SKIA_HASH={SKIA_HASH}
V8_HASH={V8_HASH}
SWE_PACKAGE_NAME={SWE_PACKAGE_NAME}
SWE_BRANDING_CHANNEL_NAME={SWE_BRANDING_CHANNEL_NAME}
"""
about_string_xml_template = """<?xml version=\"1.0\" encoding=\"utf-8\"?>
<resources xmlns:xliff=\"urn:oasis:names:tc:xliff:document:1.2\">
<!-- Text to display in about dialog -->
<string name=\"about_text\" formatted=\"false\">
{SWE_CONTACT}
Version: {SWE_VERSION}\\n
Built: {SWE_BUILD_DATE}\\n
Host: {HOST_NAME}\\n
User: {USER_NAME}\\n
Hash: {SWE_BUILD_HASH}\\n
</string>
</resources>
"""

SWE_BRANDING_FILE_SUB_PATH = "branding/BRANDING"
SWE_PACKAGE_NAME_ID = "PACKAGE_NAME"
SWE_NAME_NOT_FOUND_VALUE = "Unknown"

def run_command(cmd, path=None):
    """Runs shell command and prints the output on successful execution.
    """
    if path is None:
        path=os.path.dirname(os.path.abspath(__file__))
    output =None
    error = None
    proc = subprocess.Popen(" ".join(cmd),
                            stdout=subprocess.PIPE,
                            stderr=subprocess.PIPE,
                            cwd=path,
                            shell=True)
    output, error = proc.communicate()
    if proc.returncode != 0:
        print "--- ERROR ---"
        raise RuntimeError("""%r failed
  status code %s
  stdout %r
  stderr %r""" % (" ".join(cmd), proc.returncode, output, error))

    return output.strip()

# Inspects the command line arguments and convert each argument to an appropriate type
# in a Namespace object.
#
#   Returns:
#       object which holds the info of all the command line arguments
def parse_options():
    parser = argparse.ArgumentParser()
    parser.add_argument('-i', '--input', default=None,
                        help='Read base version FILE.')
    parser.add_argument('-o', '--output', default=None,
                        help='Write SWE strings to FILE.')
    parser.add_argument('-r', '--res', default=None,
                        help='Write SWE strings to res File.')
    parser.add_argument('-g', '--changelog', default=None,
                        help='Write SWE git log to FILE.')
    parser.add_argument('-c', '--channel', default=None,
                        help='CHANNEL to read version code. \
It could be Comma-Separated channel list.')
    parser.add_argument('--version-code-only', action='store_true',
                        help='Write versionCode information.')
    parser.add_argument('--version-string-only', action='store_true',
                        help='Write versionString information.')
    parser.add_argument('-about', '--write-about-string', default=None,
                        help='Write about page string information.')
    options = parser.parse_args()
    return options

def update_file_content(output_file, content):
    """write the given content to file if not the same already"""

    current_content=''
    create_dest_folder = False
    try:
        with open(output_file, 'r') as f:
            current_content = f.read()
    except IOError as e:
        if e.errno == 2:
            # File does not exist
            create_dest_folder = True
            pass
        else:
            raise

    if create_dest_folder == True \
            and os.path.isdir(os.path.dirname(output_file)) != True:
        os.makedirs(os.path.dirname(output_file))

    if current_content != content:
        with open(output_file, 'w') as f:
            f.write(content)

def get_last_swe_change():
    """Get the last SWE change which is merge-base between remote and local
       path (string)      : If not None, the child's current directory will be changed
                                   to cwd before it is executed.
       return: merge-base hash
    """
    gclient_revinfo_cmd = ["gclient", "revinfo"]
    gclient_revinfo = run_command(gclient_revinfo_cmd).split('\n')
    remote_branch = "remotes/origin/master"

    for revinfo in gclient_revinfo:
        pattern = r"^src:[^@]+@refs/(.+)"
        m = re.match(pattern, revinfo)
        if m != None:
            remote_branch = m.group(1)
            break

    last_swe_change_cmd = ["git", "merge-base", "HEAD", remote_branch]
    last_swe_change = run_command(last_swe_change_cmd)

    return last_swe_change

def get_value_from_name(file_path, name):
    pattern = "(.*)=(.*)"
    value = SWE_NAME_NOT_FOUND_VALUE
    with open(file_path) as f:
        for l in f:
            m = re.match(pattern, l)
            if m != None:
                if name == m.group(1).strip():
                    value = m.group(2).strip()
                    break

    return value

EPOCH_UTC = 1325376000 #2012-01-01 00:00:00

def compute_timedelta_from_epoch(commit_utc_timestamp):
    # compute the difference in time from epoch in minutes
    mins = (commit_utc_timestamp - EPOCH_UTC) / 60
    return int(mins)

def get_version_code(values_dict, timedelta_from_epoch):
    """Compute the version code which is a number always increasing.
    format:  sign_bit(1bit) MAJOR VERSION(8bits) minutes since epoch(23bits)
    """
    if 'MAJOR' not in values_dict.keys():
        raise ValueError("Major version missing")

    major_version = int (values_dict['MAJOR'])
    if major_version > 255:
        raise ValueError("Major version too big")

    # versioncode is a 32 bit value and computed as below and will always be
    # increasing as long as commits dates are incremental.
    #
    # 32 bits are formatted as below
    # | 1-empty-signbit | 8 bits major version | 23 bit for timedelta in minutes|

    MAX32BIT = 2147483647
    vc = (major_version << 23) | timedelta_from_epoch
    if vc > MAX32BIT:
        raise ValueError("VersionCode exceeds the MAX({}) value: {}".format(
                                                      MAX32BIT, vc))
    return vc

def get_epoch():
    # Find the committer date and extract year, month, date and the hour
    tip_commit_date = int(run_command(["git", "log", "-1", "--pretty=%ct"]))

    epoch_delta = compute_timedelta_from_epoch(tip_commit_date)
    return epoch_delta

def get_changelog():
    # Get 10 recent change log from git for all projects
    src = "src"
    src_absolute_path = run_command(["git", "rev-parse", "--show-toplevel"])

    gclient_revinfo = run_command(["gclient", "revinfo"])
    changelog = ""
    for project in gclient_revinfo.split("\n"):
        project_root = project.split(":")[0]
        changelog = changelog + project_root + ":\n"
        for line in run_command(["git", "log",
                                 "-n", configurations.changelog["no_of_commits"],
                                 "--pretty='%h %s'"],
                                os.path.join(src_absolute_path,
                                             "..",
                                             project_root)).split("\n"):
            changelog = changelog + "     " + line + "\n"

        changelog = changelog + "\n"
    return changelog

def get_projects_hash(browser_info):
    projects = {
                'SKIA_HASH' : 'src/third_party/skia',
                'V8_HASH'   : 'src/v8'
                }
    root_directory = run_command(['git', 'rev-parse', '--show-toplevel'])

    for p in projects.keys():
        cmd = os.path.join(root_directory, "..", projects[p])
        browser_info[p] = run_command(['git', 'log', '-1', 'HEAD', '--pretty=%H'], cmd)

def main(options):
    # Find the absolute path of this script to run all the git command
    current_path = os.path.dirname(os.path.abspath(__file__))

    # Update dictinary with all the necessary build information
    browser_info = {}
    browser_info['SWE_BUILD_DATE'] = run_command(['date'])
    browser_info['HOST_NAME']      = run_command(['hostname'])
    browser_info['USER_NAME']      = run_command(['whoami'])

    # read the input file key/value pairs ex: MAJOR=46\nMINOR=0
    if options.input is not None:
        with open(options.input) as myfile:
            for line in myfile:
                key, val = line.rstrip('\r\n').split('=', 1)
                browser_info[key] = val

    browser_info['LASTSWECHANGE'] = get_last_swe_change()

    # Use the tip commit hash for the version string
    tip_full_hash = run_command(["git", "log", "-1", "HEAD", "--pretty=%H"])
    tip_hash = tip_full_hash[:7]
    browser_info['LASTCHANGE'] = tip_full_hash

    version_code_file = None
    # Channel base directory

    swe_channels_base_paths = [os.path.join(current_path,
                                            "..",
                                            "channels",
                                            "internal"),
                               os.path.join(current_path,
                                            "..",
                                            "channels")
                               ]

    swe_branding_channel_name = "default"

    if options.channel:
        swe_channels_list = options.channel.split(",")

        for buildtype in swe_channels_list:
            for swe_channels_base_path in swe_channels_base_paths:
                t_vcf = os.path.join(swe_channels_base_path,
                                                 buildtype.strip(),
                                                 "VERSION")
                if os.path.isfile(t_vcf) == True:
                    version_code_file = t_vcf

                # TODO: We should break here otherwise VERSION file
                # in both "internal" and "external" channel folder will
                # pick up "external" only.

        swe_branding_channel_name = swe_channels_list[0]

    swe_branding_file_path = ""
    swe_branding_file_path_list = [
        os.path.join(current_path, "..", "channels", "internal",
                swe_branding_channel_name, SWE_BRANDING_FILE_SUB_PATH),
        os.path.join(current_path, "..", "channels",
                swe_branding_channel_name, SWE_BRANDING_FILE_SUB_PATH)
    ]

    for path in swe_branding_file_path_list:
        if os.path.exists(path) and os.path.isfile(path):
            swe_branding_file_path = path
            break

    version_code = get_epoch()
    browser_info['BUILDID'] = 0
    browser_info['BUILD_VERSION_CODE'] = 0
    browser_info['CHANNEL'] = options.channel
    get_projects_hash(browser_info)
    # Check the VERSION file existance in channel
    if version_code_file != None and os.path.isfile(version_code_file):
        with open(version_code_file) as vfp:
            browser_info['BUILDID'] =  int (vfp.read().strip())
            browser_info['BUILD_VERSION_CODE'] = browser_info['BUILDID']
    else:
        # Version code is our total commits from the fork point till now
        browser_info['BUILDID']     = version_code
        browser_info['BUILD_VERSION_CODE'] = get_version_code(browser_info, version_code)

    browser_info['SWE_VERSION']    = "{MAJOR}.{MINOR}.{BUILD}.{BUILDID}".format(**browser_info)
    browser_info['SWE_BUILD_HASH'] = tip_hash + " ({BUILD})".format(**browser_info)

    feedback_email = os.environ.get('SWE_APK_SUPPORT')
    if feedback_email:
        browser_info['SWE_CONTACT'] = """Please help us make your experience better by contacting the team
           at <a href="mailto:{}">{}</a>
        """.format(feedback_email, feedback_email)
    else:
        browser_info['SWE_CONTACT'] = ''

    browser_info["SWE_BRANDING_CHANNEL_NAME"] = swe_branding_channel_name
    browser_info["SWE_PACKAGE_NAME"] = get_value_from_name(swe_branding_file_path,
            SWE_PACKAGE_NAME_ID)

    # Update content for swe/VERSION
    content = content_template.format(**browser_info)

    # Text to display in about dialog
    about_string_xml = about_string_xml_template.format(**browser_info)

    if options.write_about_string is not None:
        update_file_content(options.write_about_string, about_string_xml)

    if options.output is not None:
        update_file_content(options.output, content)

    if options.res is not None:
        update_file_content(options.res, content)

    if options.channel is not None \
            and options.changelog is not None:
        for buildtype in options.channel.split(","):
            if buildtype.strip() in configurations.changelog["buildtype"]:
                update_file_content(options.changelog, get_changelog())
                break

    if options.version_code_only:
        sys.stdout.write(str(browser_info['BUILD_VERSION_CODE']))
        return

    if options.version_string_only:
        sys.stdout.write(browser_info['SWE_VERSION'])
        return

if __name__ == '__main__':
    # Parse options
    options = parse_options()
    sys.exit(main(options))
