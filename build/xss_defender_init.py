#Copyright (c) 2015, The Linux Foundation. All rights reserved.
#
#Redistribution and use in source and binary forms, with or without
#modification, are permitted provided that the following conditions are
#met:
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
#THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
#WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
#MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
#ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
#BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
#CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
#SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
#BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
#WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
#OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
#IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

import os
import urllib2
import tarfile
import urlparse
import shutil
import sys

script_dir = os.path.dirname(os.path.realpath(__file__))
chrome_src = os.path.abspath(os.path.join(script_dir, os.pardir))

dir_path = os.path.join(chrome_src, 'components/web-shield/xss_defender')

if (os.path.isdir(dir_path) == False):
  print "XSSDefender location: " + str(dir_path) + " - Create default"
  os.makedirs(dir_path)
  dest_gni_filename = os.path.join(dir_path, 'xss_defender_vars.gni')
  print "Create default gni: " + str(dest_gni_filename)
  with open(dest_gni_filename, "w+") as gni_outfile:
    gni_outfile.write("#\ndeclare_args() {\n  xss_defender_dependencies = []\n}")

else:
  print "XSSDefender location: " + str(dir_path) + " - Ok"
