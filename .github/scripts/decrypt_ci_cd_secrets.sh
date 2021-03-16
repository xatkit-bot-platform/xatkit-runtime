# This is required because Xatkit is not yet on Maven Central or similar

# Print a message
e() {
    echo -e "$1"
}

main() {
	
    e "Decrypting CI/CD secrets"
    gpg --quiet --batch --yes --decrypt --passphrase="$CI_CD_KEY" --output ./src/test/resources/xatkit-secrets.zip ./src/test/resources/xatkit-secrets.zip.gpg
    unzip ./src/test/resources/xatkit-secrets.zip -d ./src/test/resources/
}

main