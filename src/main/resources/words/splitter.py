# Get all_words.txt as a list of words
with open('all_words.txt', 'r') as f:
    all_words = f.read().splitlines()

    # Filter out words that do not contain lowercase letters
    all_words = [word.strip() for word in all_words if word.isalpha() and word.islower()]

    # Split into a list of words for each word length
    words_by_length = {}
    for word in all_words:
        if len(word) not in words_by_length:
            words_by_length[len(word)] = []
        words_by_length[len(word)].append(word)

    # Write each list of words to a file
    for length in words_by_length:
        with open(str(length) + '_letter_words.txt', 'w') as f:
            for word in words_by_length[length]:
                f.write(word + '\n')

    # Write all words to a file
    with open('all_words.txt', 'w') as f:
        for word in all_words:
            f.write(word + '\n')
