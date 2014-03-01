/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.bitlib.crypto;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

import com.mrd.bitlib.crypto.MrdExport.DecodingException;
import com.mrd.bitlib.crypto.MrdExport.V1.EncryptionParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.Header;
import com.mrd.bitlib.crypto.MrdExport.V1.InvalidChecksumException;
import com.mrd.bitlib.crypto.MrdExport.V1.KdfParameters;
import com.mrd.bitlib.crypto.MrdExport.V1.WrongNetworkException;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;

public class MrdExportTest {

   private static final String TEST_PASSWORD_1 = "foobar";
   private static final String TEST_PASSWORD_2 = "barfoo";
   private static final byte[] TEST_SALT_1 = new byte[] { 0x01, 0x02, 0x03, 0x04 };
   private static final byte[] TEST_SALT_2 = new byte[] { 0x02, 0x02, 0x03, 0x04 };
   private static final String TEST_KEY_BASE58_UNCOMPRESSED = "5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR";
   private static final String TEST_KEY_UNCOMPRESSED_ENCRYPTED = "xEncEHICAQIDBE8g0E8FRx5ImDZIWif-RleADZYxzKQYkQJJGpuch7QQaS1f8g";
   private static final String TEST_KEY_BASE58_COMPRESSED = "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7";
   private static final String TEST_KEY_COMPRESSED_ENCRYPTED = "xEncEXICAQIDBNVyfG5593-TQkkbWyV-HsF2QZFXev3ouiBuMEELJZrM6mKwjw";


   @Test
   public void passwordChecksum() {
      Assert.assertEquals('c', MrdExport.V1.calculatePasswordChecksum("abc"));
      Assert.assertEquals('v', MrdExport.V1.calculatePasswordChecksum(""));
      Assert.assertEquals('g', MrdExport.V1.calculatePasswordChecksum("a b c"));
      try {
         MrdExport.V1.calculatePasswordChecksum(null);
         fail();
      } catch (NullPointerException ignored) {
      }
   }
   /**
    * Verify that a different salt gives a different AES key
    */
   @Test
   public void testDifferentSalt() throws InterruptedException {
      // Use salt 1
      KdfParameters kdfParameters1 = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p1 = EncryptionParameters.generate(kdfParameters1);

      // Use salt 2
      KdfParameters kdfParameters2 = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_2, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p2 = EncryptionParameters.generate(kdfParameters2);
      assertFalse(BitUtils.areEqual(p1.aesKey, p2.aesKey));

   }

   /**
    * Verify that a different password gives a different AES key
    */
   @Test
   public void testDifferentPasswords() throws InterruptedException {
      // Use password 1
      KdfParameters kdfParameters1 = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p1 = EncryptionParameters.generate(kdfParameters1);

      // Use password 2
      KdfParameters kdfParameters2 = new KdfParameters(TEST_PASSWORD_2, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p2 = EncryptionParameters.generate(kdfParameters2);
      assertFalse(BitUtils.areEqual(p1.aesKey, p2.aesKey));

   }

   @Test
   public void encryptWithoutCompression() throws WrongNetworkException, InvalidChecksumException, DecodingException,
         InterruptedException {
      String base58Key = TEST_KEY_BASE58_UNCOMPRESSED;
      KdfParameters kdfParameters = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p = EncryptionParameters.generate(kdfParameters);
      String encrypted = MrdExport.V1.encrypt(p, base58Key, NetworkParameters.productionNetwork);
      assertEquals(encrypted, TEST_KEY_UNCOMPRESSED_ENCRYPTED);

      // Decrypt with reused parameters
      String decryptedKey = MrdExport.V1.decrypt(p, encrypted, NetworkParameters.productionNetwork);
      assertEquals(decryptedKey, base58Key);
   }

   @Test
   public void encryptWithCompression() throws WrongNetworkException, InvalidChecksumException, DecodingException,
         InterruptedException {
      String base58Key = TEST_KEY_BASE58_COMPRESSED;
      KdfParameters kdfParameters = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p = EncryptionParameters.generate(kdfParameters);
      String encrypted = MrdExport.V1.encrypt(p, base58Key, NetworkParameters.productionNetwork);
      assertEquals(encrypted, TEST_KEY_COMPRESSED_ENCRYPTED);

      // Decrypt with reused parameters
      String decryptedKey = MrdExport.V1.decrypt(p, encrypted, NetworkParameters.productionNetwork);
      assertEquals(decryptedKey, base58Key);
   }

   @Test
   public void wrongAesKeyInParameters() throws DecodingException, InterruptedException {
      KdfParameters kdfParameters = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p = EncryptionParameters.generate(kdfParameters);
      // Flip one bit in the AES key
      p.aesKey[0] = (byte) (p.aesKey[0] ^ 0x01);
      try {
         MrdExport.V1.decrypt(p, TEST_KEY_UNCOMPRESSED_ENCRYPTED, NetworkParameters.productionNetwork);
         fail();
      } catch (InvalidChecksumException e) {
         // expected
      }
   }

   @Test
   public void wrongNetworkInParameters() throws DecodingException, InterruptedException {
      KdfParameters kdfParameters = new KdfParameters(TEST_PASSWORD_1, TEST_SALT_1, MrdExport.V1.DEFAULT_SCRYPT_N,
            MrdExport.V1.DEFAULT_SCRYPT_R, MrdExport.V1.DEFAULT_SCRYPT_P);
      EncryptionParameters p = EncryptionParameters.generate(kdfParameters);
      try {
         MrdExport.V1.decrypt(p, TEST_KEY_UNCOMPRESSED_ENCRYPTED, NetworkParameters.testNetwork);
         fail();
      } catch (WrongNetworkException e) {
         // expected
      }

   }

   @Test
   public void generatePasswordTest() {
      StaticSimpleRandomSource randomSource = new StaticSimpleRandomSource();
      String passphrase = MrdExport.V1.generatePassword(randomSource);
      assertEquals(passphrase, "tkqilmbmbgzbsuk");
   }

   @Test
   public void headerDecodingTest() throws DecodingException {
      // Check encoding/decoding of all possible valid headers
      fullHeaderEncdingCheck(NetworkParameters.productionNetwork, false);
      fullHeaderEncdingCheck(NetworkParameters.testNetwork, false);
      fullHeaderEncdingCheck(NetworkParameters.productionNetwork, true);
      fullHeaderEncdingCheck(NetworkParameters.testNetwork, true);

      // Check that we only support version 1
      expectHeaderFail(0, 14, 1, 1);
      expectHeaderFail(2, 14, 1, 1);

      // Check that n must be between 0 and 31
      expectHeaderFail(1, -1, 1, 1);
      expectHeaderFail(1, 32, 1, 1);

      // Check that r must be between 1 and 31
      expectHeaderFail(1, 1, 0, 1);
      expectHeaderFail(1, 1, 32, 1);

      // Check that p must be between 1 and 31
      expectHeaderFail(1, 1, 1, 0);
      expectHeaderFail(1, 1, 1, 32);
   }

   private void expectHeaderFail(int version, int n, int r, int p) {
      try {
         new Header(version, NetworkParameters.productionNetwork, true, n, r, p, TEST_SALT_1);
         fail();
      } catch (RuntimeException e) {
         // expected
      }
   }

   private void fullHeaderEncdingCheck(NetworkParameters network, boolean compressed) throws DecodingException {
      for (int n = 0; n < 32; n++) {
         for (int r = 1; r < 32; r++) {
            for (int p = 1; p < 32; p++) {
               checkHeaderEncoding(new Header(1, network, compressed, n, r, p, TEST_SALT_1));
            }
         }
      }
   }

   private void checkHeaderEncoding(Header h) throws DecodingException {
      Header h2 = Header.fromBytes(h.toBytes());
      assertEquals(h, h2);
   }

}


