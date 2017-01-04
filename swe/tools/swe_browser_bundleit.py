#!/use/bin/env python

#
# Copyright (c) 2016, The Linux Foundation. All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are
# met:
#     * Redistributions of source code must retain the above copyright
#       notice, this list of conditions and the following disclaimer.
#     * Redistributions in binary form must reproduce the above
#       copyright notice, this list of conditions and the following
#       disclaimer in the documentation and/or other materials provided
#       with the distribution.
#     * Neither the name of The Linux Foundation nor the names of its
#       contributors may be used to endorse or promote products derived
#       from this software without specific prior written permission.
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
import argparse
import shutil
import zipfile
import json

def parse_args(argv):
    parser = argparse.ArgumentParser(description='Bundle SWE Broswer')

    parser.add_argument('-a', '--apk',
                        default=None,
                        help='Browser apk path with apk name')
    parser.add_argument('-l', '--applib',
                        default=None,
                        help='App dependent libraries')
    parser.add_argument('-s', '--script',
                        default=None,
                        nargs="*",
                        help='Dependent scripts')
    parser.add_argument('-x', '--exe',
                        default=None,
                        nargs="*",
                        help='Dependent executables')
    parser.add_argument('-L', '--buildlib',
                        default=None,
                        help='path for all build libraries')
    parser.add_argument('-o', '--outdir',
                        default=None,
                        help='Output directory path')
    parser.add_argument('-z', '--zip',
                        default=None,
                        help='Zip file')
    return (parser, parser.parse_args(argv))


def get_all_files(path, level=1):
    num_sep = path.count(os.path.sep)
    for root, dirs, files in os.walk(path):
        yield root, dirs, files
        num_sep_this = root.count(os.path.sep)
        if num_sep + level <= num_sep_this:
            del dirs[:]

def copylibs(src, dest, buildlibs):
    with open(src) as data_file:
        data = json.load(data_file)

    for lib in data["input-files"][0]["entries"]:
        if lib["path"].endswith(".so"):
            libname = os.path.basename(lib["path"])
            path = os.path.join(buildlibs, libname)
            if not os.path.isfile(path):
                path = libname

            shutil.copy2(path, dest)

if __name__ == '__main__':
    (parser, args) = parse_args(sys.argv[1:])

    if args.apk == None \
            or args.applib == None \
            or args.outdir == None \
            or args.buildlib == None:
        parser.print_usage()
        sys.exit(1)

    # Create output directory
    if os.path.exists(args.outdir) == True:
        shutil.rmtree(args.outdir)

    # Create subdirectories
    os.makedirs(os.path.join(args.outdir, "libs"))
    os.makedirs(os.path.join(args.outdir, "scripts"))
    os.makedirs(os.path.join(args.outdir, "executables"))

    # Copy apk
    shutil.copy2(args.apk, os.path.join(args.outdir, "Browser.apk"))

    # Copy libraries
    copylibs(args.applib,
             os.path.join(args.outdir, "libs"),
             args.buildlib)

    # Copy Scripts
    if args.script != None:
        for s in args.script:
            shutil.copy2(s, os.path.join(args.outdir, "scripts"))

    # Copy executable
    if args.exe != None:
        for e in args.exe:
            shutil.copy2(e, os.path.join(args.outdir, "executables"))

    if args.zip != None:
        if os.path.exists(args.zip) == True:
            os.remove(args.zip)
        zip = zipfile.ZipFile(args.zip, 'w', allowZip64=True)
        for root, dirs, files in os.walk(args.outdir):
            for f in files:
                zip.write(os.path.join(root, f),
                          os.path.relpath(os.path.join(root, f), args.outdir))

        zip.close();
